package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.AvailabilityCheckResult;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.AvailabilityCheckStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateAvailabilityCheckEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateAvailabilityCheckRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CurrentAvailabilityResult;

@Service
public class EligibilityPersistenceCoordinator {

	private final CertificateCancellationRequestRepository requests;
	private final CertificateAvailabilityCheckRepository checks;
	private final Duration staleAttemptThreshold;

	public EligibilityPersistenceCoordinator(
			CertificateCancellationRequestRepository requests,
			CertificateAvailabilityCheckRepository checks,
			@Value("${app.availability.stale-attempt-threshold:30s}") Duration staleAttemptThreshold) {
		this.requests = requests;
		this.checks = checks;
		this.staleAttemptThreshold = staleAttemptThreshold;
	}

	@Transactional
	public AvailabilityPreparation prepare(String dni, String correlationId) {
		Instant now = Instant.now();
		CertificateCancellationRequestEntity previous = requests.findTopByDniOrderByCreatedAtDesc(dni).orElse(null);
		if (previous != null) classifyPrevious(previous, now);

		CertificateCancellationRequestEntity request = requests.saveAndFlush(
				new CertificateCancellationRequestEntity(dni));
		request.beginAvailabilityCheck();
		requests.saveAndFlush(request);
		CertificateAvailabilityCheckEntity check = checks.saveAndFlush(new CertificateAvailabilityCheckEntity(
				request, 1, AvailabilityCheckStatus.SUBMITTED, now, correlationId));
		return new AvailabilityPreparation(request.getId(), check.getId());
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
		CertificateAvailabilityCheckEntity latest = checks
				.findFirstByRequest_IdOrderByAttemptNumberDesc(previous.getId()).orElseThrow();
		if (latest.getCheckStatus() == AvailabilityCheckStatus.SUBMITTED
				&& !latest.getRequestedAt().isBefore(now.minus(staleAttemptThreshold))) {
			throw new EligibilityInProgressException();
		}
		if (latest.getCheckStatus() == AvailabilityCheckStatus.SUBMITTED) {
			latest.fail(AvailabilityCheckResult.ERROR, now, "STALE_ATTEMPT");
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
			AvailabilityPreparation preparation, AvailabilityResult gatewayResult) {
		CertificateCancellationRequestEntity request = requests.findByIdForUpdate(preparation.requestId()).orElseThrow();
		CertificateAvailabilityCheckEntity check = checks.findByIdForUpdate(preparation.attemptId()).orElseThrow();
		if (!check.getRequest().getId().equals(request.getId())
				|| check.getCheckStatus() != AvailabilityCheckStatus.SUBMITTED
				|| request.getRequestStatus() != CancellationRequestStatus.CHECKING_AVAILABILITY) {
			throw new EligibilityConcurrencyException(
					new IllegalStateException("Eligibility attempt is no longer active"));
		}
		Instant now = Instant.now();
		AvailabilityOutcome outcome = gatewayResult.outcome();
		AvailabilityCheckResult persisted = AvailabilityCheckResult.valueOf(outcome.name());
		if (outcome == AvailabilityOutcome.UNAVAILABLE || outcome == AvailabilityOutcome.ERROR) {
			check.fail(persisted, now, gatewayResult.technicalCode());
		}
		else {
			check.complete(persisted, now, gatewayResult.externalReference());
		}
		request.recordAvailability(CurrentAvailabilityResult.valueOf(outcome.name()), statusFor(outcome));
		checks.saveAndFlush(check);
		requests.saveAndFlush(request);
		return response(request, outcome);
	}

	private CancellationRequestStatus statusFor(AvailabilityOutcome outcome) {
		return switch (outcome) {
			case AVAILABLE -> CancellationRequestStatus.PENDING_IDENTITY_VERIFICATION;
			case NOT_AVAILABLE -> CancellationRequestStatus.NO_CERTIFICATES_AVAILABLE;
			case INCONCLUSIVE, UNAVAILABLE, ERROR -> CancellationRequestStatus.STARTED;
		};
	}

	private CancellationRequestResponse response(CertificateCancellationRequestEntity request,
			AvailabilityOutcome outcome) {
		boolean canContinue = outcome == AvailabilityOutcome.AVAILABLE;
		return new CancellationRequestResponse(request.getId(), DniRule.masked(request.getDni()),
				request.getRequestStatus(), outcome, canContinue,
				canContinue ? EligibilityNextStep.IDENTITY_VERIFICATION : null);
	}

	public record AvailabilityPreparation(Long requestId, Long attemptId) {
	}
}
