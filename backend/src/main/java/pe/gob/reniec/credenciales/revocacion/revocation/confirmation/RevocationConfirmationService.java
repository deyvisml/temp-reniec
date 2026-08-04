package pe.gob.reniec.credenciales.revocacion.revocation.confirmation;

import static pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationConfirmationException.Reason.*;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.*;

@Service
public class RevocationConfirmationService {

	private static final int OTHER_MIN_LENGTH = 10;
	private static final int OTHER_MAX_LENGTH = 300;
	private final DigitalCredentialRevocationRequestRepository requests;
	private final RevocationRequestDigitalCredentialRepository digitalCredentials;
	private final IdentityVerificationRepository verifications;
	private final RevocationAuditEventRepository auditEvents;
	private final RevocationConsentCatalog consent;

	public RevocationConfirmationService(ObjectProvider<DigitalCredentialRevocationRequestRepository> requests,
			ObjectProvider<RevocationRequestDigitalCredentialRepository> digitalCredentials,
			ObjectProvider<IdentityVerificationRepository> verifications,
			ObjectProvider<RevocationAuditEventRepository> auditEvents,
			RevocationConsentCatalog consent) {
		this.requests = requests.getIfAvailable();
		this.digitalCredentials = digitalCredentials.getIfAvailable();
		this.verifications = verifications.getIfAvailable();
		this.auditEvents = auditEvents.getIfAvailable();
		this.consent = consent;
	}

	@Transactional(readOnly = true)
	public RevocationReviewResponse preview(Long requestId, RevocationReviewRequest command) {
		ensurePersistence();
		DigitalCredentialRevocationRequestEntity request = requests.findById(requestId)
				.orElseThrow(() -> failure(NOT_ALLOWED, "Request not found"));
		IdentityVerificationEntity identity = validateIdentity(requestId, true);
		if (request.getRequestStatus() != RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE) {
			throw failure(NOT_ALLOWED, "Request is not ready for review");
		}
		ValidatedDraft draft = validateDraft(command == null ? null : command.digitalCredentialUuid(),
				command == null ? null : command.statusListIndex(),
				command == null ? null : command.reasonCode(),
				command == null ? null : command.otherReason(),
				digitalCredentials.findByRequest_IdOrderByEmissionCreatedAtAscIdAsc(requestId), requestId, false);
		return response(request, identity.getVerifiedFirstName(), draft.digitalCredential(),
				draft.reason(), draft.otherReason(), false);
	}

	@Transactional(readOnly = true)
	public RevocationReviewResponse confirmed(Long requestId) {
		ensurePersistence();
		DigitalCredentialRevocationRequestEntity request = requests.findById(requestId)
				.orElseThrow(() -> failure(NOT_ALLOWED, "Request not found"));
		IdentityVerificationEntity identity = validateIdentity(requestId, false);
		if (request.getConfirmedAt() == null || request.getReasonCode() == null) {
			throw failure(NOT_ALLOWED, "Only a confirmed request can be recovered");
		}
		RevocationRequestDigitalCredentialEntity selected = selectedDigitalCredential(
				digitalCredentials.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(requestId),
				requestId);
		return response(request, identity.getVerifiedFirstName(), selected,
				request.getReasonCode(), request.getOtherReason(), true);
	}

	@Transactional
	public RevocationReviewResponse confirm(Long requestId, RevocationConfirmationRequest command,
			String correlationId) {
		ensurePersistence();
		DigitalCredentialRevocationRequestEntity request = requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> failure(NOT_ALLOWED, "Request not found"));
		IdentityVerificationEntity identity = validateIdentity(requestId, true);
		validateConsent(command);

		List<RevocationRequestDigitalCredentialEntity> current = digitalCredentials.findByRequestIdForUpdate(requestId);
		ValidatedDraft draft = validateDraft(command.digitalCredentialUuid(), command.statusListIndex(), command.reasonCode(),
				command.otherReason(), current, requestId, request.getConfirmedAt() != null);

		if (request.getConfirmedAt() != null) {
			RevocationRequestDigitalCredentialEntity selected = selectedDigitalCredential(
					current.stream().filter(RevocationRequestDigitalCredentialEntity::isSelected).toList(),
					requestId);
			if (sameDecision(request, selected, draft, command.consentVersion())) {
				return response(request, identity.getVerifiedFirstName(), selected,
						request.getReasonCode(), request.getOtherReason(), true);
			}
			throw failure(CONFLICT, "Confirmed request does not match the submitted decision");
		}

		if (request.getRequestStatus() != RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE) {
			throw failure(NOT_ALLOWED, "Request is not ready for confirmation");
		}
		if (current.stream().anyMatch(RevocationRequestDigitalCredentialEntity::isSelected)
				|| request.getReasonCode() != null || request.getOtherReason() != null) {
			throw failure(CONFLICT, "Unconfirmed request contains persisted draft data");
		}

		Instant confirmedAt = Instant.now();
		draft.digitalCredential().select(confirmedAt);
		try {
			request.confirmDecision(draft.reason(), draft.otherReason(), confirmedAt, consent.version());
		}
		catch (IllegalArgumentException | IllegalStateException exception) {
			throw failure(CONFLICT, "Request could not be confirmed");
		}
		auditEvents.save(new RevocationAuditEventEntity(request,
				RevocationAuditEventType.CONSENT_CONFIRMED,
				RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE,
				RevocationRequestStatus.CONFIRMED, consent.version(), correlationId,
				AuditEventOrigin.CITIZEN, confirmedAt));
		digitalCredentials.flush();
		requests.flush();
		return response(request, identity.getVerifiedFirstName(), draft.digitalCredential(),
				draft.reason(), draft.otherReason(), true);
	}

	private IdentityVerificationEntity validateIdentity(Long requestId, boolean requireFirstName) {
		IdentityVerificationEntity verification = verifications
				.findFirstByRequest_IdOrderByAttemptNumberDesc(requestId)
				.orElseThrow(() -> failure(IDENTITY_REQUIRED, "Verified identity is required"));
		if (verification.getVerificationStatus() != IdentityVerificationStatus.VERIFIED
				|| verification.getDniMatchResult() != IdentityMatchResult.MATCH
				|| requireFirstName && (verification.getVerifiedFirstName() == null
						|| verification.getVerifiedFirstName().isBlank())) {
			throw failure(IDENTITY_REQUIRED, "Verified identity does not match the request");
		}
		return verification;
	}

	private void validateConsent(RevocationConfirmationRequest command) {
		if (command == null || !Boolean.TRUE.equals(command.consentAccepted())) {
			throw failure(CONSENT_REQUIRED, "Consent must be accepted");
		}
		if (!consent.version().equals(command.consentVersion())) {
			throw failure(CONSENT_CHANGED, "Consent version changed");
		}
	}

	private static ValidatedDraft validateDraft(String submittedUuid, Integer submittedStatusListIndex,
			RevocationReasonCode reason,
			String submittedOtherReason, List<RevocationRequestDigitalCredentialEntity> available,
			Long requestId, boolean allowPersistedSelection) {
		String uuid = canonicalUuid(submittedUuid);
		if (submittedStatusListIndex == null || submittedStatusListIndex < 0) {
			throw failure(INVALID_SELECTION, "DigitalCredential statusListIndex is required and must not be negative");
		}
		RevocationRequestDigitalCredentialEntity digitalCredential = available.stream()
				.filter(item -> item.getAvailabilityStatus() == DigitalCredentialAvailabilityStatus.AVAILABLE
						|| (allowPersistedSelection && item.isSelected()))
				.filter(item -> item.getDigitalCredentialUuid().equals(uuid))
				.filter(item -> submittedStatusListIndex.equals(item.getStatusListIndex()))
				.filter(item -> item.getRequest().getId() != null
						&& requestId.equals(item.getRequest().getId()))
				.findFirst()
				.orElseThrow(() -> failure(INVALID_SELECTION,
						"DigitalCredential does not belong to the request or is unavailable"));
		if (reason == null || !RevocationReasonCatalog.supports(reason)) {
			throw failure(INVALID_REASON, "A controlled reason is required");
		}
		String otherReason = normalizeOtherReason(reason, submittedOtherReason);
		return new ValidatedDraft(digitalCredential, reason, otherReason);
	}

	private static String normalizeOtherReason(RevocationReasonCode reason, String value) {
		if (reason != RevocationReasonCode.OTHER) {
			if (value != null && !value.isBlank()) {
				throw failure(INVALID_REASON, "Description is only valid for OTHER");
			}
			return null;
		}
		String normalized = value == null ? "" : value.trim();
		if (normalized.length() < OTHER_MIN_LENGTH || normalized.length() > OTHER_MAX_LENGTH) {
			throw failure(INVALID_REASON,
					"OTHER description must contain between 10 and 300 characters");
		}
		return normalized;
	}

	private static String canonicalUuid(String value) {
		if (value == null) throw failure(INVALID_SELECTION, "DigitalCredential UUID is required");
		String normalized = value.toLowerCase(Locale.ROOT);
		try {
			UUID parsed = UUID.fromString(normalized);
			if (!parsed.toString().equals(normalized)) throw new IllegalArgumentException();
			return normalized;
		}
		catch (IllegalArgumentException exception) {
			throw failure(INVALID_SELECTION, "DigitalCredential UUID must use canonical format");
		}
	}

	private static RevocationRequestDigitalCredentialEntity selectedDigitalCredential(
			List<RevocationRequestDigitalCredentialEntity> selected, Long requestId) {
		if (selected.size() != 1) {
			throw failure(INVALID_SELECTION, "Exactly one selected digitalCredential is required");
		}
		RevocationRequestDigitalCredentialEntity digitalCredential = selected.getFirst();
		if (digitalCredential.getRequest().getId() == null
				|| !requestId.equals(digitalCredential.getRequest().getId())) {
			throw failure(INVALID_SELECTION, "Selected digitalCredential does not belong to the request");
		}
		return digitalCredential;
	}

	private static boolean sameDecision(DigitalCredentialRevocationRequestEntity request,
			RevocationRequestDigitalCredentialEntity selected, ValidatedDraft submitted,
			String consentVersion) {
		return selected.getDigitalCredentialUuid().equals(submitted.digitalCredential().getDigitalCredentialUuid())
				&& Objects.equals(selected.getStatusListIndex(),
						submitted.digitalCredential().getStatusListIndex())
				&& request.getReasonCode() == submitted.reason()
				&& Objects.equals(request.getOtherReason(), submitted.otherReason())
				&& Objects.equals(request.getConsentVersion(), consentVersion);
	}

	private RevocationReviewResponse response(DigitalCredentialRevocationRequestEntity request,
			String firstName,
			RevocationRequestDigitalCredentialEntity selected, RevocationReasonCode reason,
			String otherReason, boolean confirmed) {
		return new RevocationReviewResponse(request.getRequestStatus(), maskDni(request.getDni()), firstName,
				new RevocationReviewResponse.SelectedDigitalCredential(requireOfficialIndex(selected),
						selected.getEmissionCreatedAt()),
				reason, RevocationReasonCatalog.label(reason), otherReason, consent.consequences(),
				consent.text(), consent.version(), request.getConfirmedAt(), confirmed);
	}

	private static int requireOfficialIndex(RevocationRequestDigitalCredentialEntity selected) {
		if (selected.getStatusListIndex() == null) {
			throw failure(NOT_ALLOWED, "La credencial histórica no tiene un índice oficial para continuar");
		}
		return selected.getStatusListIndex();
	}

	private static String maskDni(String dni) { return "******" + dni.substring(dni.length() - 2); }
	private static RevocationConfirmationException failure(
			RevocationConfirmationException.Reason reason, String message) {
		return new RevocationConfirmationException(reason, message);
	}

	private void ensurePersistence() {
		if (requests == null || digitalCredentials == null || verifications == null || auditEvents == null) {
			throw failure(NOT_ALLOWED, "Confirmation persistence is unavailable");
		}
	}

	private record ValidatedDraft(RevocationRequestDigitalCredentialEntity digitalCredential,
			RevocationReasonCode reason, String otherReason) { }
}
