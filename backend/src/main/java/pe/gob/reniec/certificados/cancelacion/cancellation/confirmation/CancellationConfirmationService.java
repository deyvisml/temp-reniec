package pe.gob.reniec.certificados.cancelacion.cancellation.confirmation;

import static pe.gob.reniec.certificados.cancelacion.cancellation.confirmation.CancellationConfirmationException.Reason.*;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.*;

@Service
public class CancellationConfirmationService {

	private static final int OTHER_MIN_LENGTH = 10;
	private static final int OTHER_MAX_LENGTH = 300;
	private final CertificateCancellationRequestRepository requests;
	private final CancellationRequestCertificateRepository certificates;
	private final IdentityVerificationRepository verifications;
	private final CancellationAuditEventRepository auditEvents;
	private final CancellationConsentCatalog consent;

	public CancellationConfirmationService(ObjectProvider<CertificateCancellationRequestRepository> requests,
			ObjectProvider<CancellationRequestCertificateRepository> certificates,
			ObjectProvider<IdentityVerificationRepository> verifications,
			ObjectProvider<CancellationAuditEventRepository> auditEvents,
			CancellationConsentCatalog consent) {
		this.requests = requests.getIfAvailable();
		this.certificates = certificates.getIfAvailable();
		this.verifications = verifications.getIfAvailable();
		this.auditEvents = auditEvents.getIfAvailable();
		this.consent = consent;
	}

	@Transactional(readOnly = true)
	public CancellationReviewResponse preview(Long requestId, CancellationReviewRequest command) {
		ensurePersistence();
		CertificateCancellationRequestEntity request = requests.findById(requestId)
				.orElseThrow(() -> failure(NOT_ALLOWED, "Request not found"));
		validateIdentity(requestId);
		if (request.getRequestStatus() != CancellationRequestStatus.CERTIFICATES_AVAILABLE) {
			throw failure(NOT_ALLOWED, "Request is not ready for review");
		}
		ValidatedDraft draft = validateDraft(command == null ? null : command.certificateUuid(),
				command == null ? null : command.reasonCode(),
				command == null ? null : command.otherReason(),
				certificates.findByRequest_IdOrderByEmissionCreatedAtAscIdAsc(requestId), requestId, false);
		return response(request, draft.certificate(), draft.reason(), draft.otherReason(), false);
	}

	@Transactional(readOnly = true)
	public CancellationReviewResponse confirmed(Long requestId) {
		ensurePersistence();
		CertificateCancellationRequestEntity request = requests.findById(requestId)
				.orElseThrow(() -> failure(NOT_ALLOWED, "Request not found"));
		validateIdentity(requestId);
		if (request.getConfirmedAt() == null || request.getReasonCode() == null) {
			throw failure(NOT_ALLOWED, "Only a confirmed request can be recovered");
		}
		CancellationRequestCertificateEntity selected = selectedCertificate(
				certificates.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(requestId),
				requestId);
		return response(request, selected, request.getReasonCode(), request.getOtherReason(), true);
	}

	@Transactional
	public CancellationReviewResponse confirm(Long requestId, CancellationConfirmationRequest command,
			String correlationId) {
		ensurePersistence();
		CertificateCancellationRequestEntity request = requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> failure(NOT_ALLOWED, "Request not found"));
		validateIdentity(requestId);
		validateConsent(command);

		List<CancellationRequestCertificateEntity> current = certificates.findByRequestIdForUpdate(requestId);
		ValidatedDraft draft = validateDraft(command.certificateUuid(), command.reasonCode(),
				command.otherReason(), current, requestId, request.getConfirmedAt() != null);

		if (request.getConfirmedAt() != null) {
			CancellationRequestCertificateEntity selected = selectedCertificate(
					current.stream().filter(CancellationRequestCertificateEntity::isSelected).toList(),
					requestId);
			if (sameDecision(request, selected, draft, command.consentVersion())) {
				return response(request, selected, request.getReasonCode(), request.getOtherReason(), true);
			}
			throw failure(CONFLICT, "Confirmed request does not match the submitted decision");
		}

		if (request.getRequestStatus() != CancellationRequestStatus.CERTIFICATES_AVAILABLE) {
			throw failure(NOT_ALLOWED, "Request is not ready for confirmation");
		}
		if (current.stream().anyMatch(CancellationRequestCertificateEntity::isSelected)
				|| request.getReasonCode() != null || request.getOtherReason() != null) {
			throw failure(CONFLICT, "Unconfirmed request contains persisted draft data");
		}

		Instant confirmedAt = Instant.now();
		draft.certificate().select(confirmedAt);
		try {
			request.confirmDecision(draft.reason(), draft.otherReason(), confirmedAt, consent.version());
		}
		catch (IllegalArgumentException | IllegalStateException exception) {
			throw failure(CONFLICT, "Request could not be confirmed");
		}
		auditEvents.save(new CancellationAuditEventEntity(request,
				CancellationAuditEventType.CONSENT_CONFIRMED,
				CancellationRequestStatus.CERTIFICATES_AVAILABLE,
				CancellationRequestStatus.CONFIRMED, consent.version(), correlationId,
				AuditEventOrigin.CITIZEN, confirmedAt));
		certificates.flush();
		requests.flush();
		return response(request, draft.certificate(), draft.reason(), draft.otherReason(), true);
	}

	private void validateIdentity(Long requestId) {
		IdentityVerificationEntity verification = verifications
				.findFirstByRequest_IdOrderByAttemptNumberDesc(requestId)
				.orElseThrow(() -> failure(IDENTITY_REQUIRED, "Verified identity is required"));
		if (verification.getVerificationStatus() != IdentityVerificationStatus.VERIFIED
				|| verification.getDniMatchResult() != IdentityMatchResult.MATCH) {
			throw failure(IDENTITY_REQUIRED, "Verified identity does not match the request");
		}
	}

	private void validateConsent(CancellationConfirmationRequest command) {
		if (command == null || !Boolean.TRUE.equals(command.consentAccepted())) {
			throw failure(CONSENT_REQUIRED, "Consent must be accepted");
		}
		if (!consent.version().equals(command.consentVersion())) {
			throw failure(CONSENT_CHANGED, "Consent version changed");
		}
	}

	private static ValidatedDraft validateDraft(String submittedUuid, CancellationReasonCode reason,
			String submittedOtherReason, List<CancellationRequestCertificateEntity> available,
			Long requestId, boolean allowPersistedSelection) {
		String uuid = canonicalUuid(submittedUuid);
		CancellationRequestCertificateEntity certificate = available.stream()
				.filter(item -> item.getAvailabilityStatus() == CertificateAvailabilityStatus.AVAILABLE
						|| (allowPersistedSelection && item.isSelected()))
				.filter(item -> item.getCertificateUuid().equals(uuid))
				.filter(item -> item.getRequest().getId() != null
						&& requestId.equals(item.getRequest().getId()))
				.findFirst()
				.orElseThrow(() -> failure(INVALID_SELECTION,
						"Certificate does not belong to the request or is unavailable"));
		if (reason == null || !CancellationReasonCatalog.supports(reason)) {
			throw failure(INVALID_REASON, "A controlled reason is required");
		}
		String otherReason = normalizeOtherReason(reason, submittedOtherReason);
		return new ValidatedDraft(certificate, reason, otherReason);
	}

	private static String normalizeOtherReason(CancellationReasonCode reason, String value) {
		if (reason != CancellationReasonCode.OTHER) {
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
		if (value == null) throw failure(INVALID_SELECTION, "Certificate UUID is required");
		String normalized = value.toLowerCase(Locale.ROOT);
		try {
			UUID parsed = UUID.fromString(normalized);
			if (!parsed.toString().equals(normalized)) throw new IllegalArgumentException();
			return normalized;
		}
		catch (IllegalArgumentException exception) {
			throw failure(INVALID_SELECTION, "Certificate UUID must use canonical format");
		}
	}

	private static CancellationRequestCertificateEntity selectedCertificate(
			List<CancellationRequestCertificateEntity> selected, Long requestId) {
		if (selected.size() != 1) {
			throw failure(INVALID_SELECTION, "Exactly one selected certificate is required");
		}
		CancellationRequestCertificateEntity certificate = selected.getFirst();
		if (certificate.getRequest().getId() == null
				|| !requestId.equals(certificate.getRequest().getId())) {
			throw failure(INVALID_SELECTION, "Selected certificate does not belong to the request");
		}
		return certificate;
	}

	private static boolean sameDecision(CertificateCancellationRequestEntity request,
			CancellationRequestCertificateEntity selected, ValidatedDraft submitted,
			String consentVersion) {
		return selected.getCertificateUuid().equals(submitted.certificate().getCertificateUuid())
				&& request.getReasonCode() == submitted.reason()
				&& Objects.equals(request.getOtherReason(), submitted.otherReason())
				&& Objects.equals(request.getConsentVersion(), consentVersion);
	}

	private CancellationReviewResponse response(CertificateCancellationRequestEntity request,
			CancellationRequestCertificateEntity selected, CancellationReasonCode reason,
			String otherReason, boolean confirmed) {
		return new CancellationReviewResponse(request.getRequestStatus(), maskDni(request.getDni()),
				new CancellationReviewResponse.SelectedCertificate(selected.getOrderNumber(),
						selected.getEmissionCreatedAt()),
				reason, CancellationReasonCatalog.label(reason), otherReason, consent.consequences(),
				consent.text(), consent.version(), request.getConfirmedAt(), confirmed);
	}

	private static String maskDni(String dni) { return "******" + dni.substring(dni.length() - 2); }
	private static CancellationConfirmationException failure(
			CancellationConfirmationException.Reason reason, String message) {
		return new CancellationConfirmationException(reason, message);
	}

	private void ensurePersistence() {
		if (requests == null || certificates == null || verifications == null || auditEvents == null) {
			throw failure(NOT_ALLOWED, "Confirmation persistence is unavailable");
		}
	}

	private record ValidatedDraft(CancellationRequestCertificateEntity certificate,
			CancellationReasonCode reason, String otherReason) { }
}
