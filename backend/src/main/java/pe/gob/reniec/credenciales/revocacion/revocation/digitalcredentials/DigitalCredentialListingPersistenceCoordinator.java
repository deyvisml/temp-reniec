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
		if (current == RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE
				|| current == RevocationRequestStatus.DIGITAL_CREDENTIALS_SELECTED
				|| current == RevocationRequestStatus.REASON_REGISTERED
				|| current == RevocationRequestStatus.PENDING_CONFIRMATION
				|| current == RevocationRequestStatus.CONFIRMED) {
			return new Preparation(requestId, request.getDni(), snapshot, current, false);
		}
		if (current == RevocationRequestStatus.NO_DIGITAL_CREDENTIALS_AVAILABLE) {
			return new Preparation(requestId, request.getDni(), snapshot, current, false);
		}
		if (current == RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST
				&& request.getUpdatedAt().isAfter(Instant.now().minus(staleThreshold))) {
			throw new DigitalCredentialListingException(DigitalCredentialListingException.Reason.IN_PROGRESS,
					"DigitalCredential listing is already in progress");
		}
		if (current != RevocationRequestStatus.IDENTITY_VERIFIED
				&& current != RevocationRequestStatus.AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST
				&& current != RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST) {
			throw new DigitalCredentialListingException(DigitalCredentialListingException.Reason.NOT_ALLOWED,
					"Request is not ready for digitalCredential listing");
		}
		if (!snapshot.isEmpty()) {
			throw new DigitalCredentialListingException(DigitalCredentialListingException.Reason.CONFLICT,
					"Unexpected digitalCredential snapshot before listing");
		}
		RevocationRequestStatus previous = current;
		request.transitionTo(RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST, null);
		auditEvents.save(new RevocationAuditEventEntity(request,
				RevocationAuditEventType.DIGITAL_CREDENTIAL_LIST_REQUESTED, previous,
				RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST, "REQUESTED", correlationId,
				AuditEventOrigin.SYSTEM, Instant.now()));
		return new Preparation(requestId, request.getDni(), List.of(), current, true);
	}

	@Transactional
	List<RevocationRequestDigitalCredentialEntity> complete(Long requestId,
			List<DigitalCredentialListingResult.ListedDigitalCredential> listed, String correlationId) {
		DigitalCredentialRevocationRequestEntity request = lockRequest(requestId);
		ensureReserved(request);
		if (!digitalCredentials.findByRequestIdForUpdate(requestId).isEmpty()) {
			throw new DigitalCredentialListingException(DigitalCredentialListingException.Reason.CONFLICT,
					"DigitalCredential list was completed concurrently");
		}
		Instant now = Instant.now();
		if (listed.isEmpty()) {
			request.transitionTo(RevocationRequestStatus.NO_DIGITAL_CREDENTIALS_AVAILABLE, null);
			auditEvents.save(new RevocationAuditEventEntity(request,
					RevocationAuditEventType.DIGITAL_CREDENTIAL_LIST_EMPTY,
					RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST,
					RevocationRequestStatus.NO_DIGITAL_CREDENTIALS_AVAILABLE, "EMPTY", correlationId,
					AuditEventOrigin.EXTERNAL_PROVIDER, now));
			return List.of();
		}
		List<RevocationRequestDigitalCredentialEntity> entities = listed.stream()
				.map(item -> new RevocationRequestDigitalCredentialEntity(request, item.statusListIndex(),
						item.credentialType(), item.emissionCreatedAt(), item.digitalCredentialUuid(),
						item.status() == DigitalCredentialStatus.ACTIVE
								? DigitalCredentialAvailabilityStatus.AVAILABLE
								: DigitalCredentialAvailabilityStatus.REVOKED,
						item.revokedAt(), item.providerCredentialStatus(), now))
				.toList();
		List<RevocationRequestDigitalCredentialEntity> saved = digitalCredentials.saveAllAndFlush(entities);
		boolean hasActive = listed.stream().anyMatch(item -> item.status() == DigitalCredentialStatus.ACTIVE);
		RevocationRequestStatus next = hasActive ? RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE
				: RevocationRequestStatus.NO_DIGITAL_CREDENTIALS_AVAILABLE;
		request.transitionTo(next, null);
		auditEvents.save(new RevocationAuditEventEntity(request,
				hasActive ? RevocationAuditEventType.DIGITAL_CREDENTIALS_AVAILABLE
						: RevocationAuditEventType.DIGITAL_CREDENTIAL_LIST_EMPTY,
				RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST,
				next, "COUNT_" + saved.size(), correlationId,
				AuditEventOrigin.EXTERNAL_PROVIDER, now));
		return saved;
	}

	@Transactional
	void restoreAfterFailure(Long requestId, String code, String correlationId) {
		DigitalCredentialRevocationRequestEntity request = lockRequest(requestId);
		if (request.getRequestStatus() != RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST) return;
		request.transitionTo(RevocationRequestStatus.AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST, null);
		auditEvents.save(new RevocationAuditEventEntity(request,
				RevocationAuditEventType.DIGITAL_CREDENTIAL_LIST_FAILED,
				RevocationRequestStatus.CHECKING_DIGITAL_CREDENTIAL_LIST,
				RevocationRequestStatus.AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST, code, correlationId,
				AuditEventOrigin.EXTERNAL_PROVIDER, Instant.now()));
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
			RevocationRequestStatus requestStatus, boolean providerRequired) { }
}
