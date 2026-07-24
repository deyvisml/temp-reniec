package pe.gob.reniec.certificados.cancelacion.cancellation.certificates;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.*;

@Service
public class CertificateListingPersistenceCoordinator {

	private final CertificateCancellationRequestRepository requests;
	private final CancellationRequestCertificateRepository certificates;
	private final CancellationAuditEventRepository auditEvents;

	CertificateListingPersistenceCoordinator(CertificateCancellationRequestRepository requests,
			CancellationRequestCertificateRepository certificates,
			CancellationAuditEventRepository auditEvents) {
		this.requests = requests;
		this.certificates = certificates;
		this.auditEvents = auditEvents;
	}

	@Transactional
	Preparation prepare(Long requestId, String correlationId, Duration staleThreshold) {
		CertificateCancellationRequestEntity request = lockRequest(requestId);
		List<CancellationRequestCertificateEntity> snapshot = certificates
				.findByRequest_IdOrderByEmissionCreatedAtAscIdAsc(requestId);
		CancellationRequestStatus current = request.getRequestStatus();
		if (current == CancellationRequestStatus.CERTIFICATES_AVAILABLE
				|| current == CancellationRequestStatus.CERTIFICATES_SELECTED) {
			return new Preparation(requestId, request.getDni(), snapshot, false);
		}
		if (current == CancellationRequestStatus.NO_CERTIFICATES_AVAILABLE) {
			return new Preparation(requestId, request.getDni(), List.of(), false);
		}
		if (current == CancellationRequestStatus.CHECKING_CERTIFICATE_LIST
				&& request.getUpdatedAt().isAfter(Instant.now().minus(staleThreshold))) {
			throw new CertificateListingException(CertificateListingException.Reason.IN_PROGRESS,
					"Certificate listing is already in progress");
		}
		if (current != CancellationRequestStatus.IDENTITY_VERIFIED
				&& current != CancellationRequestStatus.AUTHENTICATED_PENDING_CERTIFICATE_LIST
				&& current != CancellationRequestStatus.CHECKING_CERTIFICATE_LIST) {
			throw new CertificateListingException(CertificateListingException.Reason.NOT_ALLOWED,
					"Request is not ready for certificate listing");
		}
		if (!snapshot.isEmpty()) {
			throw new CertificateListingException(CertificateListingException.Reason.CONFLICT,
					"Unexpected certificate snapshot before listing");
		}
		CancellationRequestStatus previous = current;
		request.transitionTo(CancellationRequestStatus.CHECKING_CERTIFICATE_LIST, null);
		auditEvents.save(new CancellationAuditEventEntity(request,
				CancellationAuditEventType.CERTIFICATE_LIST_REQUESTED, previous,
				CancellationRequestStatus.CHECKING_CERTIFICATE_LIST, "REQUESTED", correlationId,
				AuditEventOrigin.SYSTEM, Instant.now()));
		return new Preparation(requestId, request.getDni(), List.of(), true);
	}

	@Transactional
	List<CancellationRequestCertificateEntity> complete(Long requestId,
			List<CertificateListingResult.ListedCertificate> listed, String correlationId) {
		CertificateCancellationRequestEntity request = lockRequest(requestId);
		ensureReserved(request);
		if (!certificates.findByRequestIdForUpdate(requestId).isEmpty()) {
			throw new CertificateListingException(CertificateListingException.Reason.CONFLICT,
					"Certificate list was completed concurrently");
		}
		Instant now = Instant.now();
		if (listed.isEmpty()) {
			request.transitionTo(CancellationRequestStatus.NO_CERTIFICATES_AVAILABLE, null);
			auditEvents.save(new CancellationAuditEventEntity(request,
					CancellationAuditEventType.CERTIFICATE_LIST_EMPTY,
					CancellationRequestStatus.CHECKING_CERTIFICATE_LIST,
					CancellationRequestStatus.NO_CERTIFICATES_AVAILABLE, "EMPTY", correlationId,
					AuditEventOrigin.EXTERNAL_PROVIDER, now));
			return List.of();
		}
		List<CancellationRequestCertificateEntity> entities = listed.stream()
				.map(item -> new CancellationRequestCertificateEntity(request, item.orderNumber(),
						item.emissionCreatedAt(), item.certificateUuid(), now))
				.toList();
		List<CancellationRequestCertificateEntity> saved = certificates.saveAllAndFlush(entities);
		request.transitionTo(CancellationRequestStatus.CERTIFICATES_AVAILABLE, null);
		auditEvents.save(new CancellationAuditEventEntity(request,
				CancellationAuditEventType.CERTIFICATES_AVAILABLE,
				CancellationRequestStatus.CHECKING_CERTIFICATE_LIST,
				CancellationRequestStatus.CERTIFICATES_AVAILABLE, "COUNT_" + saved.size(), correlationId,
				AuditEventOrigin.EXTERNAL_PROVIDER, now));
		return saved;
	}

	@Transactional
	void restoreAfterFailure(Long requestId, String code, String correlationId) {
		CertificateCancellationRequestEntity request = lockRequest(requestId);
		if (request.getRequestStatus() != CancellationRequestStatus.CHECKING_CERTIFICATE_LIST) return;
		request.transitionTo(CancellationRequestStatus.AUTHENTICATED_PENDING_CERTIFICATE_LIST, null);
		auditEvents.save(new CancellationAuditEventEntity(request,
				CancellationAuditEventType.CERTIFICATE_LIST_FAILED,
				CancellationRequestStatus.CHECKING_CERTIFICATE_LIST,
				CancellationRequestStatus.AUTHENTICATED_PENDING_CERTIFICATE_LIST, code, correlationId,
				AuditEventOrigin.EXTERNAL_PROVIDER, Instant.now()));
	}

	@Transactional
	List<CancellationRequestCertificateEntity> replaceSelection(Long requestId, Set<String> selectedUuids,
			String correlationId) {
		CertificateCancellationRequestEntity request = lockRequest(requestId);
		if (request.getRequestStatus() != CancellationRequestStatus.CERTIFICATES_AVAILABLE
				&& request.getRequestStatus() != CancellationRequestStatus.CERTIFICATES_SELECTED) {
			throw new CertificateListingException(CertificateListingException.Reason.NOT_ALLOWED,
					"Request is not ready for selection");
		}
		List<CancellationRequestCertificateEntity> current = certificates.findByRequestIdForUpdate(requestId);
		Set<String> available = current.stream()
				.filter(item -> item.getAvailabilityStatus() == CertificateAvailabilityStatus.AVAILABLE)
				.map(CancellationRequestCertificateEntity::getCertificateUuid).collect(java.util.stream.Collectors.toSet());
		if (!available.containsAll(selectedUuids)) {
			throw new CertificateListingException(CertificateListingException.Reason.INVALID_SELECTION,
					"Selection contains a certificate outside the active request");
		}
		Set<String> persisted = current.stream().filter(CancellationRequestCertificateEntity::isSelected)
				.map(CancellationRequestCertificateEntity::getCertificateUuid).collect(java.util.stream.Collectors.toSet());
		if (persisted.equals(selectedUuids)) return current;
		CancellationRequestStatus previousStatus = request.getRequestStatus();
		Instant selectedAt = Instant.now();
		for (CancellationRequestCertificateEntity certificate : current) {
			if (selectedUuids.contains(certificate.getCertificateUuid())) certificate.select(selectedAt);
			else certificate.deselect();
		}
		certificates.flush();
		request.transitionTo(CancellationRequestStatus.CERTIFICATES_SELECTED, null);
		auditEvents.save(new CancellationAuditEventEntity(request,
				CancellationAuditEventType.CERTIFICATES_SELECTED,
				previousStatus,
				CancellationRequestStatus.CERTIFICATES_SELECTED, "COUNT_" + selectedUuids.size(),
				correlationId, AuditEventOrigin.CITIZEN, selectedAt));
		return current;
	}

	private CertificateCancellationRequestEntity lockRequest(Long requestId) {
		return requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> new CertificateListingException(
						CertificateListingException.Reason.NOT_ALLOWED, "Request not found"));
	}

	private static void ensureReserved(CertificateCancellationRequestEntity request) {
		if (request.getRequestStatus() != CancellationRequestStatus.CHECKING_CERTIFICATE_LIST) {
			throw new CertificateListingException(CertificateListingException.Reason.CONFLICT,
					"Certificate listing reservation is no longer active");
		}
	}

	record Preparation(Long requestId, String dni,
			List<CancellationRequestCertificateEntity> snapshot, boolean providerRequired) { }
}
