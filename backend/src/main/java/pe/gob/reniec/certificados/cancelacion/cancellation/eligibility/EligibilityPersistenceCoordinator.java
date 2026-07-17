package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateEligibilityCheckEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateEligibilityCheckRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CurrentEligibilityResult;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.EligibilityCheckResult;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.EligibilityCheckStatus;

@Service
public class EligibilityPersistenceCoordinator {

	private static final Set<CancellationRequestStatus> ACTIVE = EnumSet.of(
			CancellationRequestStatus.STARTED,
			CancellationRequestStatus.CHECKING_ELIGIBILITY,
			CancellationRequestStatus.ELIGIBLE,
			CancellationRequestStatus.PENDING_IDENTITY_VERIFICATION);

	private final CertificateCancellationRequestRepository requests;
	private final CertificateEligibilityCheckRepository checks;
	private final Duration staleAttemptThreshold;

	public EligibilityPersistenceCoordinator(
			CertificateCancellationRequestRepository requests,
			CertificateEligibilityCheckRepository checks,
			@Value("${app.eligibility.stale-attempt-threshold:30s}") Duration staleAttemptThreshold) {
		this.requests = requests;
		this.checks = checks;
		this.staleAttemptThreshold = staleAttemptThreshold;
	}

	@Transactional
	public EligibilityPreparation prepare(String dni, String correlationId) {
		Instant now = Instant.now();
		CertificateCancellationRequestEntity request = requests
				.findTopByDniAndRequestStatusInOrderByCreatedAtDesc(dni, ACTIVE)
				.orElse(null);
		boolean reused = request != null;

		if (request != null && isEligible(request)) {
			return EligibilityPreparation.recovered(response(request, EligibilityOutcome.ELIGIBLE, true));
		}

		if (request != null && request.getRequestStatus() == CancellationRequestStatus.CHECKING_ELIGIBILITY) {
			CertificateEligibilityCheckEntity latest = checks
					.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId()).orElseThrow();
			if (latest.getCheckStatus() == EligibilityCheckStatus.SUBMITTED
					&& latest.getRequestedAt().isBefore(now.minus(staleAttemptThreshold))) {
				latest.fail(EligibilityCheckResult.ERROR, now, "STALE_ATTEMPT");
				request.recordEligibility(CurrentEligibilityResult.ERROR, CancellationRequestStatus.STARTED);
				checks.saveAndFlush(latest);
				requests.saveAndFlush(request);
			}
			else {
				throw new EligibilityInProgressException();
			}
		}

		if (request == null) {
			request = requests.saveAndFlush(new CertificateCancellationRequestEntity(dni));
		}

		int attempt = checks.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId())
				.map(previous -> previous.getAttemptNumber() + 1).orElse(1);
		request.beginEligibility();
		requests.saveAndFlush(request);
		CertificateEligibilityCheckEntity check = checks.saveAndFlush(new CertificateEligibilityCheckEntity(
				request, attempt, EligibilityCheckStatus.SUBMITTED, now, correlationId));
		return EligibilityPreparation.started(request.getId(), check.getId(), reused);
	}

	@Transactional
	public CancellationRequestResponse finalizeAttempt(
			EligibilityPreparation preparation, EligibilityGatewayResult gatewayResult) {
		CertificateCancellationRequestEntity request = requests.findByIdForUpdate(preparation.requestId()).orElseThrow();
		CertificateEligibilityCheckEntity check = checks.findByIdForUpdate(preparation.attemptId()).orElseThrow();
		Instant now = Instant.now();
		EligibilityOutcome outcome = gatewayResult.outcome();
		EligibilityCheckResult persisted = EligibilityCheckResult.valueOf(outcome.name());
		if (outcome == EligibilityOutcome.UNAVAILABLE || outcome == EligibilityOutcome.ERROR) {
			check.fail(persisted, now, gatewayResult.technicalCode());
		}
		else {
			check.complete(persisted, now, gatewayResult.externalReference());
		}
		request.recordEligibility(CurrentEligibilityResult.valueOf(outcome.name()), statusFor(outcome));
		checks.saveAndFlush(check);
		requests.saveAndFlush(request);
		return response(request, outcome, preparation.reused());
	}

	private boolean isEligible(CertificateCancellationRequestEntity request) {
		return request.getEligibilityResult() == CurrentEligibilityResult.ELIGIBLE
				&& (request.getRequestStatus() == CancellationRequestStatus.ELIGIBLE
				|| request.getRequestStatus() == CancellationRequestStatus.PENDING_IDENTITY_VERIFICATION);
	}

	private CancellationRequestStatus statusFor(EligibilityOutcome outcome) {
		return switch (outcome) {
			case ELIGIBLE -> CancellationRequestStatus.PENDING_IDENTITY_VERIFICATION;
			case NOT_ELIGIBLE -> CancellationRequestStatus.NOT_ELIGIBLE;
			case INCONCLUSIVE, UNAVAILABLE, ERROR -> CancellationRequestStatus.STARTED;
		};
	}

	private CancellationRequestResponse response(CertificateCancellationRequestEntity request,
			EligibilityOutcome outcome, boolean reused) {
		boolean canContinue = outcome == EligibilityOutcome.ELIGIBLE;
		return new CancellationRequestResponse(request.getId(), DniRule.masked(request.getDni()),
				request.getRequestStatus(), outcome, canContinue,
				canContinue ? EligibilityNextStep.IDENTITY_VERIFICATION : null, reused);
	}

	public record EligibilityPreparation(
			Long requestId, Long attemptId, boolean reused, CancellationRequestResponse recoveredResponse) {

		static EligibilityPreparation started(Long requestId, Long attemptId, boolean reused) {
			return new EligibilityPreparation(requestId, attemptId, reused, null);
		}

		static EligibilityPreparation recovered(CancellationRequestResponse response) {
			return new EligibilityPreparation(null, null, true, response);
		}

		boolean recovered() { return recoveredResponse != null; }
	}
}
