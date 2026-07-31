package pe.gob.reniec.certificados.cancelacion.cancellation.execution;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import pe.gob.reniec.certificados.cancelacion.cancellation.confirmation.CancellationConfirmationException;
import pe.gob.reniec.certificados.cancelacion.cancellation.confirmation.CancellationConfirmationRequest;
import pe.gob.reniec.certificados.cancelacion.cancellation.confirmation.CancellationConfirmationService;
import pe.gob.reniec.certificados.cancelacion.cancellation.confirmation.CancellationReasonCatalog;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.*;

@Service
public class CancellationExecutionService {
	private static final Logger LOGGER = LoggerFactory.getLogger(CancellationExecutionService.class);

	private final CancellationConfirmationService confirmation;
	private final CertificateCancellationRequestRepository requests;
	private final CancellationRequestCertificateRepository certificates;
	private final RevocationOperationRepository operations;
	private final CancellationAuditEventRepository audit;
	private final RevocationGateway revocation;
	private final RevocationProperties properties;
	private final CancellationReceiptService receiptService;
	private final TransactionTemplate transactions;

	public CancellationExecutionService(CancellationConfirmationService confirmation,
			ObjectProvider<CertificateCancellationRequestRepository> requests,
			ObjectProvider<CancellationRequestCertificateRepository> certificates,
			ObjectProvider<RevocationOperationRepository> operations,
			ObjectProvider<CancellationAuditEventRepository> audit,
			RevocationGateway revocation,
			RevocationProperties properties,
			CancellationReceiptService receiptService,
			ObjectProvider<PlatformTransactionManager> transactionManager) {
		this.confirmation = confirmation;
		this.requests = requests.getIfAvailable();
		this.certificates = certificates.getIfAvailable();
		this.operations = operations.getIfAvailable();
		this.audit = audit.getIfAvailable();
		this.revocation = revocation;
		this.properties = properties;
		this.receiptService = receiptService;
		PlatformTransactionManager manager = transactionManager.getIfAvailable();
		this.transactions = manager == null ? null : new TransactionTemplate(manager);
	}

	public CancellationExecutionResponse confirmAndExecute(Long requestId,
			CancellationConfirmationRequest command, String correlationId) {
		ensurePersistence();
		ensureRevocationAvailable();
		confirmation.confirm(requestId, command, correlationId);
		return execute(requestId, correlationId);
	}

	public CancellationExecutionResponse execute(Long requestId, String correlationId) {
		ensurePersistence();
		ensureRevocationAvailable();
		Dispatch dispatch = transactions.execute(status -> prepare(requestId, correlationId));
		if (dispatch == null) {
			CancellationExecutionResponse snapshot = current(requestId);
			if (snapshot.requestStatus() == CancellationRequestStatus.REVOCATION_SUCCEEDED) {
				receiptService.generate(requestId, correlationId);
			}
			return current(requestId);
		}
		RevocationGateway.Result result;
		try {
			result = revocation.revoke(dispatch.certificateUuid(), dispatch.idempotencyKey());
		}
		catch (RuntimeException exception) {
			LOGGER.warn("Revocation provider outcome is unknown requestId={} operationId={}",
					requestId, dispatch.operationId(), exception);
			result = new RevocationGateway.Result(RevocationResult.OUTCOME_UNKNOWN, null,
					"PROVIDER_UNAVAILABLE", Instant.now());
		}
		RevocationGateway.Result finalResult = result;
		transactions.executeWithoutResult(status -> complete(dispatch, finalResult, correlationId));
		if (result.outcome() == RevocationResult.SUCCEEDED) {
			receiptService.generate(requestId, correlationId);
		}
		return current(requestId);
	}

	public CancellationExecutionResponse retryReceipt(Long requestId, String correlationId) {
		ensurePersistence();
		receiptService.generate(requestId, correlationId);
		return current(requestId);
	}

	public CancellationExecutionResponse current(Long requestId) {
		ensurePersistence();
		return transactions.execute(status -> snapshot(requestId));
	}

	public byte[] receiptDocument(Long requestId) {
		ensurePersistence();
		return receiptService.document(requestId);
	}

	private Dispatch prepare(Long requestId, String correlationId) {
		CertificateCancellationRequestEntity request = requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> notAllowed("Request not found"));
		RevocationOperationEntity existing = operations
				.findFirstByRequest_IdOrderByAttemptNumberDesc(requestId).orElse(null);
		if (existing != null && existing.getOperationStatus() != RevocationOperationStatus.PREPARED
				&& existing.getOperationStatus() != RevocationOperationStatus.SUBMITTED
				&& existing.getOperationStatus() != RevocationOperationStatus.OUTCOME_UNKNOWN) {
			return null;
		}
		if (request.getRequestStatus() != CancellationRequestStatus.CONFIRMED
				&& request.getRequestStatus() != CancellationRequestStatus.REVOCATION_IN_PROGRESS
				&& request.getRequestStatus() != CancellationRequestStatus.REVOCATION_OUTCOME_UNKNOWN) {
			return null;
		}
		CancellationRequestCertificateEntity selected = selected(requestId, true);
		Instant now = Instant.now();
		if (existing != null && existing.getOperationStatus() == RevocationOperationStatus.SUBMITTED
				&& existing.getUpdatedAt().plus(properties.getStaleSubmissionThreshold()).isAfter(now)) {
			return null;
		}
		RevocationOperationEntity operation = existing;
		if (operation == null) {
			String key = "cancel-request-" + requestId;
			operation = operations.saveAndFlush(new RevocationOperationEntity(
					request, key, 1, now, correlationId));
			audit.save(new CancellationAuditEventEntity(request,
					CancellationAuditEventType.REVOCATION_PREPARED,
					request.getRequestStatus(), CancellationRequestStatus.REVOCATION_IN_PROGRESS,
					null, correlationId, AuditEventOrigin.BACKEND, now));
		}
		if (operation.getOperationStatus() == RevocationOperationStatus.PREPARED) {
			operation.markSubmitted(now, null);
			audit.save(new CancellationAuditEventEntity(request,
					CancellationAuditEventType.REVOCATION_SUBMITTED,
					CancellationRequestStatus.REVOCATION_IN_PROGRESS,
					CancellationRequestStatus.REVOCATION_IN_PROGRESS, null,
					correlationId, AuditEventOrigin.EXTERNAL_PROVIDER, now));
		}
		request.transitionTo(CancellationRequestStatus.REVOCATION_IN_PROGRESS, null);
		return new Dispatch(operation.getId(), operation.getIdempotencyKey(),
				selected.getCertificateUuid(), requestId);
	}

	private void complete(Dispatch dispatch, RevocationGateway.Result result, String correlationId) {
		CertificateCancellationRequestEntity request = requests.findByIdForUpdate(dispatch.requestId())
				.orElseThrow(() -> notAllowed("Request not found"));
		RevocationOperationEntity operation = operations.findById(dispatch.operationId())
				.orElseThrow(() -> notAllowed("Revocation operation not found"));
		if (operation.getOperationStatus() != RevocationOperationStatus.PREPARED
				&& operation.getOperationStatus() != RevocationOperationStatus.SUBMITTED
				&& operation.getOperationStatus() != RevocationOperationStatus.OUTCOME_UNKNOWN) return;

		Instant completed = result.respondedAt() == null ? Instant.now() : result.respondedAt();
		RevocationOperationStatus operationStatus = RevocationOperationStatus.valueOf(result.outcome().name());
		operation.complete(operationStatus, result.outcome(), completed, completed,
				result.externalReference(), result.errorCode());
		selected(dispatch.requestId(), true).applyAtomicOutcome(result.outcome());
		CancellationRequestStatus previous = request.getRequestStatus();
		CancellationRequestStatus next = switch (result.outcome()) {
			case SUCCEEDED -> CancellationRequestStatus.REVOCATION_SUCCEEDED;
			case FAILED -> CancellationRequestStatus.REVOCATION_FAILED;
			case OUTCOME_UNKNOWN -> CancellationRequestStatus.REVOCATION_OUTCOME_UNKNOWN;
		};
		CancellationFinalOutcome outcome = switch (result.outcome()) {
			case SUCCEEDED -> CancellationFinalOutcome.REVOCATION_SUCCEEDED;
			case FAILED -> CancellationFinalOutcome.REVOCATION_FAILED;
			case OUTCOME_UNKNOWN -> CancellationFinalOutcome.OUTCOME_UNKNOWN;
		};
		request.transitionTo(next, outcome);
		audit.save(new CancellationAuditEventEntity(request,
				result.outcome() == RevocationResult.OUTCOME_UNKNOWN
						? CancellationAuditEventType.OUTCOME_UNKNOWN
						: CancellationAuditEventType.REVOCATION_CONFIRMED,
				previous, next, result.outcome().name(), correlationId,
				AuditEventOrigin.EXTERNAL_PROVIDER, completed));
	}

	private CancellationExecutionResponse snapshot(Long requestId) {
		CertificateCancellationRequestEntity request = requireRequest(requestId);
		CancellationRequestCertificateEntity selected = selected(requestId, false);
		RevocationOperationEntity operation = operations
				.findFirstByRequest_IdOrderByAttemptNumberDesc(requestId).orElse(null);
		CancellationReceiptService.Snapshot receipt = receiptService.snapshot(requestId);
		CancellationExecutionState state = switch (request.getRequestStatus()) {
			case RECEIPT_AVAILABLE -> CancellationExecutionState.SUCCEEDED;
			case REVOCATION_FAILED, FAILED -> CancellationExecutionState.FAILED;
			case REVOCATION_OUTCOME_UNKNOWN, OUTCOME_UNKNOWN -> CancellationExecutionState.OUTCOME_UNKNOWN;
			case REVOCATION_SUCCEEDED, COMPLETED -> receipt != null
					&& receipt.status() == ReceiptGenerationStatus.FAILED
							? CancellationExecutionState.RECEIPT_FAILED
							: CancellationExecutionState.PROCESSING;
			default -> CancellationExecutionState.PROCESSING;
		};
		CancellationExecutionResponse.Receipt receiptResponse = receipt == null ? null
				: new CancellationExecutionResponse.Receipt(receipt.code(),
						receipt.status(), receipt.availableAt(), receipt.downloadAvailable());
		return new CancellationExecutionResponse(state, request.getRequestStatus(),
				maskDni(request.getDni()),
				new CancellationExecutionResponse.Certificate(selected.getOrderNumber(),
						selected.getEmissionCreatedAt()),
				CancellationReasonCatalog.label(request.getReasonCode()), request.getOtherReason(),
				request.getConfirmedAt(), operation == null ? null : operation.getCompletedAt(),
				receiptResponse);
	}

	private CertificateCancellationRequestEntity requireRequest(Long requestId) {
		return requests.findById(requestId).orElseThrow(() -> notAllowed("Request not found"));
	}

	private CancellationRequestCertificateEntity selected(Long requestId, boolean lock) {
		List<CancellationRequestCertificateEntity> items = lock
				? certificates.findByRequestIdForUpdate(requestId)
				: certificates.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(requestId);
		List<CancellationRequestCertificateEntity> selected = lock
				? items.stream().filter(CancellationRequestCertificateEntity::isSelected).toList()
				: items;
		if (selected.size() != 1) throw notAllowed("Exactly one selected certificate is required");
		return selected.getFirst();
	}

	private static String maskDni(String dni) { return "******" + dni.substring(dni.length() - 2); }
	private static CancellationConfirmationException notAllowed(String message) {
		return new CancellationConfirmationException(
				CancellationConfirmationException.Reason.NOT_ALLOWED, message);
	}

	private void ensurePersistence() {
		if (requests == null || certificates == null || operations == null
				|| audit == null || transactions == null) {
			throw notAllowed("Cancellation execution is unavailable");
		}
	}

	private void ensureRevocationAvailable() {
		if (!revocation.isAvailable()) {
			throw new CancellationConfirmationException(
					CancellationConfirmationException.Reason.DEPENDENCY_UNAVAILABLE,
					"Revocation integration is unavailable");
		}
	}

	private record Dispatch(Long operationId, String idempotencyKey,
			String certificateUuid, Long requestId) { }
}
