package pe.gob.reniec.credenciales.revocacion.revocation.initiation;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.AvailabilityCheckResult;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.AvailabilityCheckStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialAvailabilityCheckEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialAvailabilityCheckRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.CurrentAvailabilityResult;

@Service
public class AvailabilityPersistenceCoordinator {

	private final DigitalCredentialRevocationRequestRepository requests;
	private final DigitalCredentialAvailabilityCheckRepository checks;
	private final Duration staleAttemptThreshold;

	public AvailabilityPersistenceCoordinator(
			DigitalCredentialRevocationRequestRepository requests,
			DigitalCredentialAvailabilityCheckRepository checks,
			@Value("${app.availability.stale-attempt-threshold:30s}") Duration staleAttemptThreshold) {
		this.requests = requests;
		this.checks = checks;
		if (staleAttemptThreshold == null || staleAttemptThreshold.isZero() || staleAttemptThreshold.isNegative()) {
			throw new IllegalArgumentException("app.availability.stale-attempt-threshold must be greater than zero");
		}
		this.staleAttemptThreshold = staleAttemptThreshold;
	}

	@Transactional
	public AvailabilityPreparation prepare(String dni, String correlationId) {
		Instant now = Instant.now();
		DigitalCredentialRevocationRequestEntity previous = requests.findTopByDniOrderByCreatedAtDesc(dni).orElse(null);
		if (previous != null) classifyPrevious(previous, now);

		DigitalCredentialRevocationRequestEntity request = requests.saveAndFlush(
				new DigitalCredentialRevocationRequestEntity(dni));
		request.beginAvailabilityCheck();
		requests.saveAndFlush(request);
		DigitalCredentialAvailabilityCheckEntity check = checks.saveAndFlush(new DigitalCredentialAvailabilityCheckEntity(
				request, 1, AvailabilityCheckStatus.SUBMITTED, now, correlationId));
		return new AvailabilityPreparation(request.getId(), check.getId());
	}

	private void classifyPrevious(DigitalCredentialRevocationRequestEntity previous, Instant now) {
		RevocationRequestStatus status = previous.getRequestStatus();
		if (RevocationRequestInitiationPolicy.isProtected(status)) {
			throw new RevocationRequestProtectedException();
		}
		if (RevocationRequestInitiationPolicy.isAvailabilityCheckInProgress(status)) {
			closeStaleAvailabilityCheckOrReject(previous, now);
			return;
		}
		if (RevocationRequestInitiationPolicy.isReplaceable(status)) {
			abandon(previous);
			return;
		}
		if (!RevocationRequestInitiationPolicy.isTerminalHistory(status)) {
			throw new IllegalStateException("Unclassified revocation request status: " + status);
		}
	}

	private void closeStaleAvailabilityCheckOrReject(DigitalCredentialRevocationRequestEntity previous, Instant now) {
		DigitalCredentialAvailabilityCheckEntity latest = checks
				.findFirstByRequest_IdOrderByAttemptNumberDesc(previous.getId()).orElseThrow();
		if (latest.getCheckStatus() == AvailabilityCheckStatus.SUBMITTED
				&& !latest.getRequestedAt().isBefore(now.minus(staleAttemptThreshold))) {
			throw new AvailabilityCheckInProgressException();
		}
		if (latest.getCheckStatus() == AvailabilityCheckStatus.SUBMITTED) {
			latest.fail(AvailabilityCheckResult.ERROR, now, "STALE_ATTEMPT");
			checks.saveAndFlush(latest);
		}
		abandon(previous);
	}

	private void abandon(DigitalCredentialRevocationRequestEntity request) {
		request.transitionTo(RevocationRequestStatus.ABANDONED, null);
		requests.saveAndFlush(request);
	}

	@Transactional
	public RevocationRequestResponse finalizeAttempt(
			AvailabilityPreparation preparation, AvailabilityResult gatewayResult) {
		Objects.requireNonNull(preparation, "preparation");
		Objects.requireNonNull(gatewayResult, "gatewayResult");
		DigitalCredentialRevocationRequestEntity request = requests.findByIdForUpdate(preparation.requestId()).orElseThrow();
		DigitalCredentialAvailabilityCheckEntity check = checks.findByIdForUpdate(preparation.attemptId()).orElseThrow();
		if (!check.getRequest().getId().equals(request.getId())
				|| check.getCheckStatus() != AvailabilityCheckStatus.SUBMITTED
				|| request.getRequestStatus() != RevocationRequestStatus.CHECKING_AVAILABILITY) {
			throw new RevocationRequestConcurrencyException(
					new IllegalStateException("Availability check is no longer active"));
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

	private RevocationRequestStatus statusFor(AvailabilityOutcome outcome) {
		return switch (outcome) {
			case AVAILABLE -> RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION;
			case NOT_AVAILABLE -> RevocationRequestStatus.NO_DIGITAL_CREDENTIALS_AVAILABLE;
			case INCONCLUSIVE, UNAVAILABLE, ERROR -> RevocationRequestStatus.STARTED;
		};
	}

	private RevocationRequestResponse response(DigitalCredentialRevocationRequestEntity request,
			AvailabilityOutcome outcome) {
		boolean canContinue = outcome == AvailabilityOutcome.AVAILABLE;
		return new RevocationRequestResponse(request.getId(), DniRule.masked(request.getDni()),
				request.getRequestStatus(), outcome, canContinue,
				canContinue ? RevocationRequestNextStep.IDENTITY_VERIFICATION : null);
	}

	public record AvailabilityPreparation(Long requestId, Long attemptId) {
	}
}
