package pe.gob.reniec.certificados.cancelacion.cancellation.reason;

import static pe.gob.reniec.certificados.cancelacion.cancellation.reason.CancellationReasonException.Reason.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.ObjectProvider;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.*;

@Service
public class CancellationReasonService {
	private static final int OTHER_MIN_LENGTH = 10;
	private static final int OTHER_MAX_LENGTH = 300;
	private static final Set<CancellationRequestStatus> EDITABLE = Set.of(
			CancellationRequestStatus.CERTIFICATES_SELECTED,
			CancellationRequestStatus.REASON_REGISTERED,
			CancellationRequestStatus.PENDING_CONFIRMATION);

	private final CertificateCancellationRequestRepository requests;
	private final CancellationRequestCertificateRepository certificates;
	private final IdentityVerificationRepository verifications;
	private final CancellationAuditEventRepository auditEvents;

	public CancellationReasonService(ObjectProvider<CertificateCancellationRequestRepository> requests,
			ObjectProvider<CancellationRequestCertificateRepository> certificates,
			ObjectProvider<IdentityVerificationRepository> verifications,
			ObjectProvider<CancellationAuditEventRepository> auditEvents) {
		this.requests = requests.getIfAvailable();
		this.certificates = certificates.getIfAvailable();
		this.verifications = verifications.getIfAvailable();
		this.auditEvents = auditEvents.getIfAvailable();
	}

	@Transactional(readOnly = true)
	public CancellationReasonResponse current(Long requestId) {
		ensurePersistence();
		CertificateCancellationRequestEntity request = requests.findById(requestId)
				.orElseThrow(() -> failure(NOT_ALLOWED, "Request not found"));
		validateIdentity(requestId);
		validateSelection(requestId,
				certificates.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(requestId));
		if (!EDITABLE.contains(request.getRequestStatus())) {
			throw failure(NOT_ALLOWED, "Request is not ready for a reason");
		}
		return response(request);
	}

	@Transactional
	public CancellationReasonResponse register(Long requestId, CancellationReasonRequest command,
			String correlationId) {
		ensurePersistence();
		CertificateCancellationRequestEntity request = requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> failure(NOT_ALLOWED, "Request not found"));
		validateIdentity(requestId);
		validateSelection(requestId, certificates.findByRequestIdForUpdate(requestId).stream()
				.filter(CancellationRequestCertificateEntity::isSelected).toList());
		if (!EDITABLE.contains(request.getRequestStatus())) {
			throw failure(NOT_ALLOWED, "Request is not ready for a reason");
		}

		CancellationReasonCode reason = command == null ? null : command.reasonCode();
		String description = normalizeDescription(reason, command == null ? null : command.otherReason());
		CancellationRequestStatus previous = request.getRequestStatus();
		try {
			request.registerReason(reason, description);
		}
		catch (IllegalArgumentException | IllegalStateException exception) {
			throw failure(INVALID_REASON, exception.getMessage());
		}
		auditEvents.save(new CancellationAuditEventEntity(request,
				CancellationAuditEventType.REASON_REGISTERED, previous,
				CancellationRequestStatus.REASON_REGISTERED, reason.name(), correlationId,
				AuditEventOrigin.CITIZEN, Instant.now()));
		return response(request);
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

	private static void validateSelection(Long requestId, List<CancellationRequestCertificateEntity> selected) {
		if (selected.size() != 1) {
			throw failure(INVALID_SELECTION, "Exactly one selected certificate is required");
		}
		CancellationRequestCertificateEntity certificate = selected.getFirst();
		if (certificate.getAvailabilityStatus() != CertificateAvailabilityStatus.AVAILABLE
				|| certificate.getRequest().getId() == null
				|| !requestId.equals(certificate.getRequest().getId())) {
			throw failure(INVALID_SELECTION, "Selected certificate is not valid for the request");
		}
	}

	private static String normalizeDescription(CancellationReasonCode reason, String value) {
		if (reason == null) throw failure(INVALID_REASON, "A reason is required");
		if (reason != CancellationReasonCode.OTHER) {
			if (value != null && !value.isBlank()) {
				throw failure(INVALID_REASON, "Description is only valid for OTHER");
			}
			return null;
		}
		String normalized = value == null ? "" : value.trim();
		if (normalized.length() < OTHER_MIN_LENGTH || normalized.length() > OTHER_MAX_LENGTH) {
			throw failure(INVALID_REASON, "OTHER description must contain between 10 and 300 characters");
		}
		return normalized;
	}

	private static CancellationReasonResponse response(CertificateCancellationRequestEntity request) {
		return new CancellationReasonResponse(request.getRequestStatus().name(), request.getReasonCode(),
				request.getOtherReason(), request.getReasonCode() != null, "CONFIRMATION");
	}

	private static CancellationReasonException failure(CancellationReasonException.Reason reason,
			String message) {
		return new CancellationReasonException(reason, message);
	}

	private void ensurePersistence() {
		if (requests == null || certificates == null || verifications == null || auditEvents == null) {
			throw failure(NOT_ALLOWED, "Reason persistence is unavailable");
		}
	}
}
