package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import java.time.Duration;
import java.time.Instant;

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
		CertificateCancellationRequestEntity previous = requests.findTopByDniOrderByCreatedAtDesc(dni).orElse(null);
		if (previous != null) classifyPrevious(previous, now);

		CertificateCancellationRequestEntity request = requests.saveAndFlush(
				new CertificateCancellationRequestEntity(dni));
		request.beginEligibility();
		requests.saveAndFlush(request);
		CertificateEligibilityCheckEntity check = checks.saveAndFlush(new CertificateEligibilityCheckEntity(
				request, 1, EligibilityCheckStatus.SUBMITTED, now, correlationId));
		return new EligibilityPreparation(request.getId(), check.getId());
	}

	private void classifyPrevious(CertificateCancellationRequestEntity previous, Instant now) {
		CancellationRequestStatus status = previous.getRequestStatus();
		if (CancellationRequestInitiationPolicy.isProtected(status)) {
			throw new CancellationRequestProtectedException();
		}
		if (CancellationRequestInitiationPolicy.isEligibilityInProgress(status)) {
			closeStaleEligibilityOrReject(previous, now);
			return;
		}
		if (CancellationRequestInitiationPolicy.isReplaceable(status)) {
			abandon(previous);
			return;
		}
		if (!CancellationRequestInitiationPolicy.isTerminalHistory(status)) {
			throw new IllegalStateException("Unclassified cancellation request status: " + status);
		}
	}

	private void closeStaleEligibilityOrReject(CertificateCancellationRequestEntity previous, Instant now) {
		CertificateEligibilityCheckEntity latest = checks
				.findFirstByRequest_IdOrderByAttemptNumberDesc(previous.getId()).orElseThrow();
		if (latest.getCheckStatus() == EligibilityCheckStatus.SUBMITTED
				&& !latest.getRequestedAt().isBefore(now.minus(staleAttemptThreshold))) {
			throw new EligibilityInProgressException();
		}
		if (latest.getCheckStatus() == EligibilityCheckStatus.SUBMITTED) {
			latest.fail(EligibilityCheckResult.ERROR, now, "STALE_ATTEMPT");
			checks.saveAndFlush(latest);
		}
		abandon(previous);
	}

	private void abandon(CertificateCancellationRequestEntity request) {
		request.transitionTo(CancellationRequestStatus.ABANDONED, null);
		requests.saveAndFlush(request);
	}

	@Transactional
	public CancellationRequestResponse finalizeAttempt(
			EligibilityPreparation preparation, EligibilityGatewayResult gatewayResult) {
		CertificateCancellationRequestEntity request = requests.findByIdForUpdate(preparation.requestId()).orElseThrow();
		CertificateEligibilityCheckEntity check = checks.findByIdForUpdate(preparation.attemptId()).orElseThrow();
		if (!check.getRequest().getId().equals(request.getId())
				|| check.getCheckStatus() != EligibilityCheckStatus.SUBMITTED
				|| request.getRequestStatus() != CancellationRequestStatus.CHECKING_ELIGIBILITY) {
			throw new EligibilityConcurrencyException(
					new IllegalStateException("Eligibility attempt is no longer active"));
		}
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
		return response(request, outcome);
	}

	private CancellationRequestStatus statusFor(EligibilityOutcome outcome) {
		return switch (outcome) {
			case ELIGIBLE -> CancellationRequestStatus.PENDING_IDENTITY_VERIFICATION;
			case NOT_ELIGIBLE -> CancellationRequestStatus.NOT_ELIGIBLE;
			case INCONCLUSIVE, UNAVAILABLE, ERROR -> CancellationRequestStatus.STARTED;
		};
	}

	private CancellationRequestResponse response(CertificateCancellationRequestEntity request,
			EligibilityOutcome outcome) {
		boolean canContinue = outcome == EligibilityOutcome.ELIGIBLE;
		return new CancellationRequestResponse(request.getId(), DniRule.masked(request.getDni()),
				request.getRequestStatus(), outcome, canContinue,
				canContinue ? EligibilityNextStep.IDENTITY_VERIFICATION : null);
	}

	public record EligibilityPreparation(Long requestId, Long attemptId) {
	}
}
