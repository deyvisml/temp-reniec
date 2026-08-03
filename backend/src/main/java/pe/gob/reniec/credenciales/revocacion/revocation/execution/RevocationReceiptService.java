package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationConfirmationException;
import pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationReasonCatalog;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.AuditEventOrigin;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationAuditEventEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationAuditEventRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationAuditEventType;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationFinalOutcome;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReceiptEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReceiptRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestDigitalCredentialEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestDigitalCredentialRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.ReceiptGenerationStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityMatchResult;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationStatus;

@Service
public class RevocationReceiptService {
	private static final ZoneId LIMA = ZoneId.of("America/Lima");

	private final DigitalCredentialRevocationRequestRepository requests;
	private final RevocationRequestDigitalCredentialRepository digitalCredentials;
	private final RevocationOperationRepository operations;
	private final RevocationReceiptRepository receipts;
	private final RevocationAuditEventRepository audit;
	private final IdentityVerificationRepository verifications;
	private final ReceiptStorage storage;
	private final RevocationReceiptPdfRenderer pdf;
	private final ReceiptProperties properties;
	private final RevocationProperties revocationProperties;
	private final Clock clock;
	private final TransactionTemplate transactions;

	public RevocationReceiptService(
			ObjectProvider<DigitalCredentialRevocationRequestRepository> requests,
			ObjectProvider<RevocationRequestDigitalCredentialRepository> digitalCredentials,
			ObjectProvider<RevocationOperationRepository> operations,
			ObjectProvider<RevocationReceiptRepository> receipts,
			ObjectProvider<RevocationAuditEventRepository> audit,
			ObjectProvider<IdentityVerificationRepository> verifications,
			ReceiptStorage storage,
			RevocationReceiptPdfRenderer pdf,
			ReceiptProperties properties,
			RevocationProperties revocationProperties,
			ObjectProvider<Clock> clock,
			ObjectProvider<PlatformTransactionManager> transactionManager) {
		this.requests = requests.getIfAvailable();
		this.digitalCredentials = digitalCredentials.getIfAvailable();
		this.operations = operations.getIfAvailable();
		this.receipts = receipts.getIfAvailable();
		this.audit = audit.getIfAvailable();
		this.verifications = verifications.getIfAvailable();
		this.storage = storage;
		this.pdf = pdf;
		this.properties = properties;
		this.revocationProperties = revocationProperties;
		Clock configuredClock = clock.getIfAvailable();
		this.clock = configuredClock == null ? Clock.systemUTC() : configuredClock;
		PlatformTransactionManager manager = transactionManager.getIfAvailable();
		this.transactions = manager == null ? null : new TransactionTemplate(manager);
	}

	public void generate(Long requestId, String correlationId) {
		ensurePersistence();
		ReceiptDraft draft = transactions.execute(status -> prepare(requestId, correlationId));
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
			DigitalCredentialRevocationRequestEntity request = requireRequest(requestId);
			if (request.getRequestStatus() != RevocationRequestStatus.RECEIPT_AVAILABLE) {
				throw notAllowed("Receipt is not available");
			}
			return receipts.findFirstByRequest_IdAndGenerationStatusOrderByAvailableAtDesc(
							requestId, ReceiptGenerationStatus.AVAILABLE)
					.map(RevocationReceiptEntity::getStorageReference)
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

	private ReceiptDraft prepare(Long requestId, String correlationId) {
		DigitalCredentialRevocationRequestEntity request = requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> notAllowed("Request not found"));
		if (request.getFinalOutcome() != RevocationFinalOutcome.REVOCATION_SUCCEEDED) {
			throw notAllowed("A successful revocation is required");
		}
		RevocationOperationEntity operation = operations
				.findFirstByRequest_IdOrderByAttemptNumberDesc(requestId)
				.filter(RevocationOperationEntity::isSucceeded)
				.orElseThrow(() -> notAllowed("Successful revocation not found"));
		RevocationReceiptEntity receipt = receipts
				.findFirstByRequest_IdOrderByCreatedAtDesc(requestId).orElse(null);
		Instant now = clock.instant();
		if (receipt != null && receipt.getGenerationStatus() == ReceiptGenerationStatus.AVAILABLE) {
			return null;
		}
		if (receipt == null) {
			String code = "RV-" + ZonedDateTime.ofInstant(now, LIMA).getYear()
					+ "-" + String.format("%06d", requestId);
			receipt = receipts.saveAndFlush(new RevocationReceiptEntity(request, operation, code));
		}
		Instant propagationReadyAt = operation.getCompletedAt()
				.plus(revocationProperties.getPropagationDelay());
		if (now.isBefore(propagationReadyAt)) {
			return null;
		}
		if (receipt != null && receipt.getGenerationStatus() == ReceiptGenerationStatus.GENERATING
				&& receipt.getUpdatedAt().plus(properties.getStaleGenerationThreshold()).isAfter(now)) {
			return null;
		}
		String verifiedFirstName = verifications
				.findFirstByRequest_IdOrderByAttemptNumberDesc(requestId)
				.filter(verification -> verification.getVerificationStatus() == IdentityVerificationStatus.VERIFIED)
				.filter(verification -> verification.getDniMatchResult() == IdentityMatchResult.MATCH)
				.map(verification -> verification.getVerifiedFirstName())
				.filter(name -> !name.isBlank())
				.orElse(null);
		if (verifiedFirstName == null) {
			markIdentityNameUnavailable(request, receipt, correlationId);
			return null;
		}
		receipt.markGenerating();
		RevocationRequestDigitalCredentialEntity selected = selected(requestId);
		RevocationReceiptPdfRenderer.Data data = new RevocationReceiptPdfRenderer.Data(
				receipt.getReceiptCode(), request.getDni(), verifiedFirstName,
				requireOfficialIndex(selected),
				selected.getEmissionCreatedAt(), RevocationReasonCatalog.label(request.getReasonCode()),
				request.getOtherReason(), request.getConfirmedAt(), operation.getCompletedAt());
		return new ReceiptDraft(receipt.getId(), receipt.getReceiptCode(), data);
	}

	private static int requireOfficialIndex(RevocationRequestDigitalCredentialEntity selected) {
		if (selected.getStatusListIndex() == null) {
			throw new IllegalStateException("Historical credential has no official status list index");
		}
		return selected.getStatusListIndex();
	}

	private void markIdentityNameUnavailable(DigitalCredentialRevocationRequestEntity request,
			RevocationReceiptEntity receipt, String correlationId) {
		if (receipt.getGenerationStatus() == ReceiptGenerationStatus.FAILED
				&& "IDENTITY_NAME_UNAVAILABLE".equals(receipt.getErrorCode())) return;
		receipt.markFailed("IDENTITY_NAME_UNAVAILABLE");
		audit.save(new RevocationAuditEventEntity(request,
				RevocationAuditEventType.RECEIPT_GENERATION_FAILED,
				request.getRequestStatus(), request.getRequestStatus(), "IDENTITY_NAME_UNAVAILABLE",
				correlationId, AuditEventOrigin.BACKEND, clock.instant()));
	}

	private void markAvailable(Long requestId, Long receiptId, String reference,
			String correlationId) {
		DigitalCredentialRevocationRequestEntity request = requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> notAllowed("Request not found"));
		RevocationReceiptEntity receipt = receipts.findById(receiptId)
				.orElseThrow(() -> notAllowed("Receipt not found"));
		if (receipt.getGenerationStatus() == ReceiptGenerationStatus.AVAILABLE) return;
		Instant now = clock.instant();
		receipt.markAvailable(reference, now, now);
		RevocationRequestStatus previous = request.getRequestStatus();
		request.transitionTo(RevocationRequestStatus.RECEIPT_AVAILABLE,
				RevocationFinalOutcome.REVOCATION_SUCCEEDED);
		audit.save(new RevocationAuditEventEntity(request,
				RevocationAuditEventType.RECEIPT_GENERATED, previous,
				RevocationRequestStatus.RECEIPT_AVAILABLE, receipt.getReceiptCode(),
				correlationId, AuditEventOrigin.BACKEND, now));
	}

	private void markFailed(Long requestId, Long receiptId, String correlationId) {
		DigitalCredentialRevocationRequestEntity request = requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> notAllowed("Request not found"));
		RevocationReceiptEntity receipt = receipts.findById(receiptId)
				.orElseThrow(() -> notAllowed("Receipt not found"));
		if (receipt.getGenerationStatus() == ReceiptGenerationStatus.AVAILABLE) return;
		receipt.markFailed("PDF_GENERATION_FAILED");
		audit.save(new RevocationAuditEventEntity(request,
				RevocationAuditEventType.RECEIPT_GENERATION_FAILED,
				request.getRequestStatus(), request.getRequestStatus(), "PDF_GENERATION_FAILED",
				correlationId, AuditEventOrigin.BACKEND, clock.instant()));
	}

	private RevocationRequestDigitalCredentialEntity selected(Long requestId) {
		var selected = digitalCredentials
				.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(requestId);
		if (selected.size() != 1) {
			throw notAllowed("Exactly one selected digitalCredential is required");
		}
		return selected.getFirst();
	}

	private DigitalCredentialRevocationRequestEntity requireRequest(Long requestId) {
		return requests.findById(requestId).orElseThrow(() -> notAllowed("Request not found"));
	}

	private static RevocationConfirmationException notAllowed(String message) {
		return new RevocationConfirmationException(
				RevocationConfirmationException.Reason.NOT_ALLOWED, message);
	}

	private void ensurePersistence() {
		if (requests == null || digitalCredentials == null || operations == null || receipts == null
				|| audit == null || verifications == null || transactions == null) {
			throw notAllowed("Receipt persistence is unavailable");
		}
	}

	public record Snapshot(String code, ReceiptGenerationStatus status, Instant availableAt) {
		public boolean downloadAvailable() {
			return status == ReceiptGenerationStatus.AVAILABLE;
		}
	}

	private record ReceiptDraft(Long receiptId, String receiptCode,
			RevocationReceiptPdfRenderer.Data data) {
	}
}
