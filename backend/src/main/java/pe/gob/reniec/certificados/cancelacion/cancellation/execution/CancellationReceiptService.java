package pe.gob.reniec.certificados.cancelacion.cancellation.execution;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import pe.gob.reniec.certificados.cancelacion.cancellation.confirmation.CancellationConfirmationException;
import pe.gob.reniec.certificados.cancelacion.cancellation.confirmation.CancellationReasonCatalog;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.AuditEventOrigin;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationAuditEventEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationAuditEventRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationAuditEventType;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationFinalOutcome;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReceiptEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReceiptRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestCertificateEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestCertificateRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.ReceiptGenerationStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationRepository;

@Service
public class CancellationReceiptService {
	private static final ZoneId LIMA = ZoneId.of("America/Lima");

	private final CertificateCancellationRequestRepository requests;
	private final CancellationRequestCertificateRepository certificates;
	private final RevocationOperationRepository operations;
	private final CancellationReceiptRepository receipts;
	private final CancellationAuditEventRepository audit;
	private final ReceiptStorage storage;
	private final CancellationReceiptPdfRenderer pdf;
	private final ReceiptProperties properties;
	private final TransactionTemplate transactions;

	public CancellationReceiptService(
			ObjectProvider<CertificateCancellationRequestRepository> requests,
			ObjectProvider<CancellationRequestCertificateRepository> certificates,
			ObjectProvider<RevocationOperationRepository> operations,
			ObjectProvider<CancellationReceiptRepository> receipts,
			ObjectProvider<CancellationAuditEventRepository> audit,
			ReceiptStorage storage,
			CancellationReceiptPdfRenderer pdf,
			ReceiptProperties properties,
			ObjectProvider<PlatformTransactionManager> transactionManager) {
		this.requests = requests.getIfAvailable();
		this.certificates = certificates.getIfAvailable();
		this.operations = operations.getIfAvailable();
		this.receipts = receipts.getIfAvailable();
		this.audit = audit.getIfAvailable();
		this.storage = storage;
		this.pdf = pdf;
		this.properties = properties;
		PlatformTransactionManager manager = transactionManager.getIfAvailable();
		this.transactions = manager == null ? null : new TransactionTemplate(manager);
	}

	public void generate(Long requestId, String correlationId) {
		ensurePersistence();
		ReceiptDraft draft = transactions.execute(status -> prepare(requestId));
		if (draft == null) return;
		try {
			byte[] document = pdf.render(draft.data());
			String reference = storage.store(draft.receiptCode(), document);
			transactions.executeWithoutResult(status ->
					markAvailable(requestId, draft.receiptId(), reference, correlationId));
		}
		catch (IOException | RuntimeException exception) {
			transactions.executeWithoutResult(status ->
					markFailed(requestId, draft.receiptId(), correlationId));
		}
	}

	public byte[] document(Long requestId) {
		ensurePersistence();
		String reference = transactions.execute(status -> {
			CertificateCancellationRequestEntity request = requireRequest(requestId);
			if (request.getRequestStatus() != CancellationRequestStatus.RECEIPT_AVAILABLE) {
				throw notAllowed("Receipt is not available");
			}
			return receipts.findFirstByRequest_IdAndGenerationStatusOrderByAvailableAtDesc(
							requestId, ReceiptGenerationStatus.AVAILABLE)
					.map(CancellationReceiptEntity::getStorageReference)
					.orElseThrow(() -> notAllowed("Receipt is not available"));
		});
		try {
			return storage.read(reference);
		}
		catch (IOException exception) {
			throw notAllowed("Receipt document could not be read");
		}
	}

	public Snapshot snapshot(Long requestId) {
		ensurePersistence();
		return transactions.execute(status -> receipts.findFirstByRequest_IdOrderByCreatedAtDesc(requestId)
				.map(receipt -> new Snapshot(receipt.getReceiptCode(),
						receipt.getGenerationStatus(), receipt.getAvailableAt()))
				.orElse(null));
	}

	private ReceiptDraft prepare(Long requestId) {
		CertificateCancellationRequestEntity request = requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> notAllowed("Request not found"));
		if (request.getFinalOutcome() != CancellationFinalOutcome.REVOCATION_SUCCEEDED) {
			throw notAllowed("A successful revocation is required");
		}
		RevocationOperationEntity operation = operations
				.findFirstByRequest_IdOrderByAttemptNumberDesc(requestId)
				.filter(RevocationOperationEntity::isSucceeded)
				.orElseThrow(() -> notAllowed("Successful revocation not found"));
		CancellationReceiptEntity receipt = receipts
				.findFirstByRequest_IdOrderByCreatedAtDesc(requestId).orElse(null);
		Instant now = Instant.now();
		if (receipt != null && receipt.getGenerationStatus() == ReceiptGenerationStatus.AVAILABLE) {
			return null;
		}
		if (receipt != null && receipt.getGenerationStatus() == ReceiptGenerationStatus.GENERATING
				&& receipt.getUpdatedAt().plus(properties.getStaleGenerationThreshold()).isAfter(now)) {
			return null;
		}
		if (receipt == null) {
			String code = "CD-" + ZonedDateTime.ofInstant(now, LIMA).getYear()
					+ "-" + String.format("%06d", requestId);
			receipt = receipts.saveAndFlush(new CancellationReceiptEntity(request, operation, code));
		}
		receipt.markGenerating();
		CancellationRequestCertificateEntity selected = selected(requestId);
		CancellationReceiptPdfRenderer.Data data = new CancellationReceiptPdfRenderer.Data(
				receipt.getReceiptCode(), maskDni(request.getDni()), selected.getOrderNumber(),
				selected.getEmissionCreatedAt(), CancellationReasonCatalog.label(request.getReasonCode()),
				request.getOtherReason(), request.getConfirmedAt(), operation.getCompletedAt());
		return new ReceiptDraft(receipt.getId(), receipt.getReceiptCode(), data);
	}

	private void markAvailable(Long requestId, Long receiptId, String reference,
			String correlationId) {
		CertificateCancellationRequestEntity request = requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> notAllowed("Request not found"));
		CancellationReceiptEntity receipt = receipts.findById(receiptId)
				.orElseThrow(() -> notAllowed("Receipt not found"));
		if (receipt.getGenerationStatus() == ReceiptGenerationStatus.AVAILABLE) return;
		Instant now = Instant.now();
		receipt.markAvailable(reference, now, now);
		CancellationRequestStatus previous = request.getRequestStatus();
		request.transitionTo(CancellationRequestStatus.RECEIPT_AVAILABLE,
				CancellationFinalOutcome.REVOCATION_SUCCEEDED);
		audit.save(new CancellationAuditEventEntity(request,
				CancellationAuditEventType.RECEIPT_GENERATED, previous,
				CancellationRequestStatus.RECEIPT_AVAILABLE, receipt.getReceiptCode(),
				correlationId, AuditEventOrigin.BACKEND, now));
	}

	private void markFailed(Long requestId, Long receiptId, String correlationId) {
		CertificateCancellationRequestEntity request = requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> notAllowed("Request not found"));
		CancellationReceiptEntity receipt = receipts.findById(receiptId)
				.orElseThrow(() -> notAllowed("Receipt not found"));
		if (receipt.getGenerationStatus() == ReceiptGenerationStatus.AVAILABLE) return;
		receipt.markFailed("PDF_GENERATION_FAILED");
		audit.save(new CancellationAuditEventEntity(request,
				CancellationAuditEventType.RECEIPT_GENERATION_FAILED,
				request.getRequestStatus(), request.getRequestStatus(), "PDF_GENERATION_FAILED",
				correlationId, AuditEventOrigin.BACKEND, Instant.now()));
	}

	private CancellationRequestCertificateEntity selected(Long requestId) {
		var selected = certificates
				.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(requestId);
		if (selected.size() != 1) {
			throw notAllowed("Exactly one selected certificate is required");
		}
		return selected.getFirst();
	}

	private CertificateCancellationRequestEntity requireRequest(Long requestId) {
		return requests.findById(requestId).orElseThrow(() -> notAllowed("Request not found"));
	}

	private static String maskDni(String dni) {
		return "******" + dni.substring(dni.length() - 2);
	}

	private static CancellationConfirmationException notAllowed(String message) {
		return new CancellationConfirmationException(
				CancellationConfirmationException.Reason.NOT_ALLOWED, message);
	}

	private void ensurePersistence() {
		if (requests == null || certificates == null || operations == null || receipts == null
				|| audit == null || transactions == null) {
			throw notAllowed("Receipt persistence is unavailable");
		}
	}

	public record Snapshot(String code, ReceiptGenerationStatus status, Instant availableAt) {
		public boolean downloadAvailable() {
			return status == ReceiptGenerationStatus.AVAILABLE;
		}
	}

	private record ReceiptDraft(Long receiptId, String receiptCode,
			CancellationReceiptPdfRenderer.Data data) {
	}
}
