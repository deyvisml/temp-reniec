package pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.*;

@Service
public class DigitalCredentialListingPersistenceCoordinator {

	private final DigitalCredentialRevocationRequestRepository requests;
	private final RevocationRequestDigitalCredentialRepository digitalCredentials;
	private final RevocationAuditEventRepository auditEvents;
	private final IdentityVerificationRepository verifications;

	DigitalCredentialListingPersistenceCoordinator(DigitalCredentialRevocationRequestRepository requests,
			RevocationRequestDigitalCredentialRepository digitalCredentials,
			RevocationAuditEventRepository auditEvents,
			IdentityVerificationRepository verifications) {
		this.requests = requests;
		this.digitalCredentials = digitalCredentials;
		this.auditEvents = auditEvents;
		this.verifications = verifications;
	}

	@Transactional
	Preparation prepare(Long requestId, String correlationId, Duration staleThreshold) {
		DigitalCredentialRevocationRequestEntity request = lockRequest(requestId);
		requireCompleteIdentity(requestId);
		List<RevocationRequestDigitalCredentialEntity> snapshot = digitalCredentials
				.findByRequest_IdOrderByEmissionCreatedAtAscIdAsc(requestId);
		RevocationRequestStatus current = request.getRequestStatus();
		if (isFrozen(current)) {
			return new Preparation(requestId, request.getDni(), snapshot, current, current, false);
		}
		if (current == RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST
				&& request.getUpdatedAt().isAfter(Instant.now().minus(staleThreshold))) {
			throw new DigitalCredentialListingException(DigitalCredentialListingException.Reason.IN_PROGRESS,
					"DigitalCredential listing is already in progress");
		}
		RevocationRequestStatus previous = current == RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST
				? statusFor(snapshot) : current;
		if (!isRefreshable(previous)) {
			throw new DigitalCredentialListingException(DigitalCredentialListingException.Reason.NOT_ALLOWED,
					"Request is not ready for digitalCredential listing");
		}
		request.transitionTo(RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST, null);
		auditEvents.save(new RevocationAuditEventEntity(request,
				RevocationAuditEventType.DIGITAL_CREDENTIAL_LIST_REQUESTED, previous,
				RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST, "REQUESTED", correlationId,
				AuditEventOrigin.SYSTEM, Instant.now()));
		return new Preparation(requestId, request.getDni(), snapshot, current, previous, true);
	}

	@Transactional
	List<RevocationRequestDigitalCredentialEntity> complete(Long requestId,
			List<DigitalCredentialListingResult.ListedDigitalCredential> listed, String correlationId) {
		DigitalCredentialRevocationRequestEntity request = lockRequest(requestId);
		ensureReserved(request);
		List<RevocationRequestDigitalCredentialEntity> previous =
				digitalCredentials.findByRequestIdForUpdate(requestId);
		List<RevocationRequestDigitalCredentialEntity> saved = replaceSnapshot(request, previous, listed);
		Instant now = Instant.now();
		boolean hasActive = listed.stream().anyMatch(item -> item.status() == DigitalCredentialStatus.ACTIVE);
		RevocationRequestStatus next = hasActive ? RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE
				: RevocationRequestStatus.NO_DIGITAL_CREDENTIALS_AVAILABLE;
		request.transitionTo(next, null);
		auditEvents.save(new RevocationAuditEventEntity(request,
				!previous.isEmpty() ? RevocationAuditEventType.DIGITAL_CREDENTIAL_LIST_REFRESHED
						: hasActive ? RevocationAuditEventType.DIGITAL_CREDENTIALS_AVAILABLE
								: RevocationAuditEventType.DIGITAL_CREDENTIAL_LIST_EMPTY,
				RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST,
				next, listed.isEmpty() ? "EMPTY" : "COUNT_" + saved.size(), correlationId,
				AuditEventOrigin.EXTERNAL_PROVIDER, now));
		return saved;
	}

	@Transactional
	RevalidationCompletion completeForConfirmation(Long requestId,
			List<DigitalCredentialListingResult.ListedDigitalCredential> listed,
			String selectedUuid, int selectedStatusListIndex, String correlationId) {
		DigitalCredentialRevocationRequestEntity request = lockRequest(requestId);
		ensureReserved(request);
		List<RevocationRequestDigitalCredentialEntity> previous =
				digitalCredentials.findByRequestIdForUpdate(requestId);
		List<RevocationRequestDigitalCredentialEntity> saved = replaceSnapshot(request, previous, listed);
		boolean selectedIsCurrent = saved.stream().anyMatch(item ->
				item.getAvailabilityStatus() == DigitalCredentialAvailabilityStatus.AVAILABLE
						&& item.getDigitalCredentialUuid().equals(selectedUuid)
						&& item.getStatusListIndex() != null
						&& item.getStatusListIndex() == selectedStatusListIndex);
		boolean hasActive = saved.stream().anyMatch(item ->
				item.getAvailabilityStatus() == DigitalCredentialAvailabilityStatus.AVAILABLE);
		RevocationRequestStatus next = selectedIsCurrent ? RevocationRequestStatus.PENDING_CONFIRMATION
				: hasActive ? RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE
						: RevocationRequestStatus.NO_DIGITAL_CREDENTIALS_AVAILABLE;
		request.transitionTo(next, null);
		auditEvents.save(new RevocationAuditEventEntity(request,
				selectedIsCurrent ? RevocationAuditEventType.DIGITAL_CREDENTIAL_LIST_REVALIDATED
						: RevocationAuditEventType.DIGITAL_CREDENTIAL_SELECTION_STALE,
				RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST, next,
				selectedIsCurrent ? "CURRENT" : "STALE", correlationId,
				AuditEventOrigin.EXTERNAL_PROVIDER, Instant.now()));
		return new RevalidationCompletion(saved, selectedIsCurrent);
	}

	private List<RevocationRequestDigitalCredentialEntity> replaceSnapshot(
			DigitalCredentialRevocationRequestEntity request,
			List<RevocationRequestDigitalCredentialEntity> previous,
			List<DigitalCredentialListingResult.ListedDigitalCredential> listed) {
		digitalCredentials.deleteAll(previous);
		digitalCredentials.flush();
		Instant consultedAt = Instant.now();
		List<RevocationRequestDigitalCredentialEntity> entities = listed.stream()
				.map(item -> new RevocationRequestDigitalCredentialEntity(request, item.statusListIndex(),
						item.credentialType(), item.emissionCreatedAt(), item.digitalCredentialUuid(),
						item.status() == DigitalCredentialStatus.ACTIVE
								? DigitalCredentialAvailabilityStatus.AVAILABLE
								: DigitalCredentialAvailabilityStatus.REVOKED,
						item.revokedAt(), item.providerCredentialStatus(), consultedAt))
				.toList();
		return entities.isEmpty() ? List.of() : digitalCredentials.saveAllAndFlush(entities);
	}

	@Transactional
	void restoreAfterFailure(Long requestId, RevocationRequestStatus previousStatus,
			String code, String correlationId) {
		DigitalCredentialRevocationRequestEntity request = lockRequest(requestId);
		if (request.getRequestStatus() != RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST) return;
		RevocationRequestStatus restored = isRefreshable(previousStatus)
				? previousStatus : RevocationRequestStatus.AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST;
		request.transitionTo(restored, null);
		auditEvents.save(new RevocationAuditEventEntity(request,
				RevocationAuditEventType.DIGITAL_CREDENTIAL_LIST_FAILED,
				RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST,
				restored, code, correlationId,
				AuditEventOrigin.EXTERNAL_PROVIDER, Instant.now()));
	}

	private static boolean isFrozen(RevocationRequestStatus status) {
		return switch (status) {
			case CONFIRMED, REVOCATION_IN_PROGRESS, REVOCATION_SUCCEEDED, REVOCATION_FAILED,
					REVOCATION_OUTCOME_UNKNOWN, COMPLETED, FAILED, OUTCOME_UNKNOWN,
					RECEIPT_AVAILABLE, ABANDONED -> true;
			default -> false;
		};
	}

	private static boolean isRefreshable(RevocationRequestStatus status) {
		return switch (status) {
			case IDENTITY_VERIFIED, AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST,
					DIGITAL_CREDENTIALS_AVAILABLE, NO_DIGITAL_CREDENTIALS_AVAILABLE,
					DIGITAL_CREDENTIALS_SELECTED, REASON_REGISTERED, PENDING_CONFIRMATION -> true;
			default -> false;
		};
	}

	private static RevocationRequestStatus statusFor(List<RevocationRequestDigitalCredentialEntity> snapshot) {
		if (snapshot.isEmpty()) return RevocationRequestStatus.AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST;
		return snapshot.stream().anyMatch(item ->
				item.getAvailabilityStatus() == DigitalCredentialAvailabilityStatus.AVAILABLE)
				? RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE
				: RevocationRequestStatus.NO_DIGITAL_CREDENTIALS_AVAILABLE;
	}

	private DigitalCredentialRevocationRequestEntity lockRequest(Long requestId) {
		return requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> new DigitalCredentialListingException(
						DigitalCredentialListingException.Reason.NOT_ALLOWED, "Request not found"));
	}

	private void requireCompleteIdentity(Long requestId) {
		IdentityVerificationEntity verification = verifications
				.findFirstByRequest_IdOrderByAttemptNumberDesc(requestId)
				.orElseThrow(() -> new DigitalCredentialListingException(
						DigitalCredentialListingException.Reason.NOT_ALLOWED,
						"Verified identity is required"));
		if (verification.getVerificationStatus() != IdentityVerificationStatus.VERIFIED
				|| verification.getDniMatchResult() != IdentityMatchResult.MATCH
				|| verification.getVerifiedFirstName() == null
				|| verification.getVerifiedFirstName().isBlank()) {
			throw new DigitalCredentialListingException(
					DigitalCredentialListingException.Reason.NOT_ALLOWED,
					"Complete verified identity is required");
		}
	}

	private static void ensureReserved(DigitalCredentialRevocationRequestEntity request) {
		if (request.getRequestStatus() != RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST) {
			throw new DigitalCredentialListingException(DigitalCredentialListingException.Reason.CONFLICT,
					"DigitalCredential listing reservation is no longer active");
		}
	}

	record Preparation(Long requestId, String dni,
			List<RevocationRequestDigitalCredentialEntity> snapshot,
			RevocationRequestStatus requestStatus, RevocationRequestStatus previousStatus,
			boolean providerRequired) { }

	record RevalidationCompletion(List<RevocationRequestDigitalCredentialEntity> snapshot,
			boolean selectedIsCurrent) { }
}
