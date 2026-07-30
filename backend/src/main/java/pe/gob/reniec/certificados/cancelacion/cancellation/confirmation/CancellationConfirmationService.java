package pe.gob.reniec.certificados.cancelacion.cancellation.confirmation;

import static pe.gob.reniec.certificados.cancelacion.cancellation.confirmation.CancellationConfirmationException.Reason.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.*;

@Service
public class CancellationConfirmationService {

	private static final Set<CancellationRequestStatus> REVIEWABLE = Set.of(
			CancellationRequestStatus.REASON_REGISTERED,
			CancellationRequestStatus.PENDING_CONFIRMATION,
			CancellationRequestStatus.CONFIRMED);

	private static final Map<CancellationReasonCode, String> REASON_LABELS = Map.of(
			CancellationReasonCode.THEFT, "Robo",
			CancellationReasonCode.LOSS, "Pérdida",
			CancellationReasonCode.DEVICE_OR_NUMBER_CHANGE, "Cambio de equipo o número",
			CancellationReasonCode.SUSPECTED_UNAUTHORIZED_USE, "Sospecha de uso no autorizado",
			CancellationReasonCode.OTHER, "Otro motivo");

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
	public CancellationReviewResponse review(Long requestId) {
		ensurePersistence();
		CertificateCancellationRequestEntity request = requests.findById(requestId)
				.orElseThrow(() -> failure(NOT_ALLOWED, "Request not found"));
		validateIdentity(requestId);
		validateReviewable(request);
		CancellationRequestCertificateEntity selected = selectedCertificate(requestId);
		return response(request, selected);
	}

	@Transactional
	public CancellationReviewResponse confirm(Long requestId, CancellationConfirmationRequest command,
			String correlationId) {
		ensurePersistence();
		CertificateCancellationRequestEntity request = requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> failure(NOT_ALLOWED, "Request not found"));
		validateIdentity(requestId);
		validateConsent(command);

		if (request.getRequestStatus() == CancellationRequestStatus.CONFIRMED) {
			if (request.getConfirmedAt() != null && consent.version().equals(request.getConsentVersion())) {
				return response(request, selectedCertificateForUpdate(requestId));
			}
			throw failure(CONFLICT, "Confirmed request does not match current consent");
		}

		validateReviewable(request);
		CancellationRequestCertificateEntity selected = selectedCertificateForUpdate(requestId);
		CancellationRequestStatus previous = request.getRequestStatus();
		Instant confirmedAt = Instant.now();
		request.confirm(confirmedAt, consent.version());
		auditEvents.save(new CancellationAuditEventEntity(request,
				CancellationAuditEventType.CONSENT_CONFIRMED, previous,
				CancellationRequestStatus.CONFIRMED, consent.version(), correlationId,
				AuditEventOrigin.CITIZEN, confirmedAt));
		return response(request, selected);
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

	private static void validateReviewable(CertificateCancellationRequestEntity request) {
		if (!REVIEWABLE.contains(request.getRequestStatus())) {
			throw failure(NOT_ALLOWED, "Request is not ready for confirmation");
		}
		if (request.getReasonCode() == null || !REASON_LABELS.containsKey(request.getReasonCode())) {
			throw failure(INVALID_REASON, "A valid reason is required");
		}
		if (request.getReasonCode() == CancellationReasonCode.OTHER
				&& (request.getOtherReason() == null || request.getOtherReason().isBlank())) {
			throw failure(INVALID_REASON, "OTHER requires a description");
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

	private CancellationRequestCertificateEntity selectedCertificate(Long requestId) {
		return validateSelected(requestId, certificates
				.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(requestId));
	}

	private CancellationRequestCertificateEntity selectedCertificateForUpdate(Long requestId) {
		return validateSelected(requestId, certificates.findByRequestIdForUpdate(requestId).stream()
				.filter(CancellationRequestCertificateEntity::isSelected).toList());
	}

	private static CancellationRequestCertificateEntity validateSelected(Long requestId,
			List<CancellationRequestCertificateEntity> selected) {
		if (selected.size() != 1 || selected.stream().anyMatch(certificate ->
				certificate.getAvailabilityStatus() != CertificateAvailabilityStatus.AVAILABLE
						|| certificate.getRequest().getId() == null
						|| !requestId.equals(certificate.getRequest().getId()))) {
			throw failure(INVALID_SELECTION, "Exactly one selected available certificate is required");
		}
		return selected.getFirst();
	}

	private CancellationReviewResponse response(CertificateCancellationRequestEntity request,
			CancellationRequestCertificateEntity selected) {
		return new CancellationReviewResponse(request.getRequestStatus().name(), maskDni(request.getDni()),
				new CancellationReviewResponse.SelectedCertificate(selected.getOrderNumber(),
						selected.getEmissionCreatedAt(), maskUuid(selected.getCertificateUuid())),
				request.getReasonCode().name(), REASON_LABELS.get(request.getReasonCode()),
				request.getOtherReason(), consent.consequences(), consent.text(), consent.version(),
				request.getConfirmedAt(), request.getRequestStatus() == CancellationRequestStatus.CONFIRMED);
	}

	private static String maskDni(String dni) { return "******" + dni.substring(dni.length() - 2); }
	private static String maskUuid(String uuid) { return uuid.substring(0, 8) + "…" + uuid.substring(uuid.length() - 4); }
	private static CancellationConfirmationException failure(
			CancellationConfirmationException.Reason reason, String message) {
		return new CancellationConfirmationException(reason, message);
	}

	private void ensurePersistence() {
		if (requests == null || certificates == null || verifications == null || auditEvents == null) {
			throw failure(NOT_ALLOWED, "Confirmation persistence is unavailable");
		}
	}
}
