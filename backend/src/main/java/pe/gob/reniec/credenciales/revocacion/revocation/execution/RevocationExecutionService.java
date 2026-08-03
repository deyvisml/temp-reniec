package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import java.time.Instant;
import java.time.Clock;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationConfirmationException;
import pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationConfirmationRequest;
import pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationConfirmationService;
import pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationReasonCatalog;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.*;

@Service
public class RevocationExecutionService {
	private static final Logger LOGGER = LoggerFactory.getLogger(RevocationExecutionService.class);

	private final RevocationConfirmationService confirmation;
	private final DigitalCredentialRevocationRequestRepository requests;
	private final RevocationRequestDigitalCredentialRepository digitalCredentials;
	private final RevocationOperationRepository operations;
	private final RevocationAuditEventRepository audit;
	private final IdentityVerificationRepository verifications;
	private final RevocationGateway revocation;
	private final RevocationProperties properties;
	private final RevocationReceiptService receiptService;
	private final TransactionTemplate transactions;
	private final Clock clock;

	public RevocationExecutionService(RevocationConfirmationService confirmation,
			ObjectProvider<DigitalCredentialRevocationRequestRepository> requests,
			ObjectProvider<RevocationRequestDigitalCredentialRepository> digitalCredentials,
			ObjectProvider<RevocationOperationRepository> operations,
			ObjectProvider<RevocationAuditEventRepository> audit,
			ObjectProvider<IdentityVerificationRepository> verifications,
			RevocationGateway revocation,
			RevocationProperties properties,
			RevocationReceiptService receiptService,
			ObjectProvider<Clock> clock,
			ObjectProvider<PlatformTransactionManager> transactionManager) {
		this.confirmation = confirmation;
		this.requests = requests.getIfAvailable();
		this.digitalCredentials = digitalCredentials.getIfAvailable();
		this.operations = operations.getIfAvailable();
		this.audit = audit.getIfAvailable();
		this.verifications = verifications.getIfAvailable();
		this.revocation = revocation;
		this.properties = properties;
		this.receiptService = receiptService;
		Clock configuredClock = clock.getIfAvailable();
		this.clock = configuredClock == null ? Clock.systemUTC() : configuredClock;
		PlatformTransactionManager manager = transactionManager.getIfAvailable();
		this.transactions = manager == null ? null : new TransactionTemplate(manager);
	}

	public RevocationExecutionResponse confirmAndExecute(Long requestId,
			RevocationConfirmationRequest command, String correlationId) {
		ensurePersistence();
		ensureRevocationAvailable();
		confirmation.confirm(requestId, command, correlationId);
		return execute(requestId, correlationId);
	}

	public RevocationExecutionResponse execute(Long requestId, String correlationId) {
		ensurePersistence();
		ensureRevocationAvailable();
		Dispatch dispatch = transactions.execute(status -> prepare(requestId, correlationId));
		if (dispatch == null) {
			RevocationExecutionResponse snapshot = current(requestId);
			if (snapshot.requestStatus() == RevocationRequestStatus.REVOCATION_SUCCEEDED) {
				receiptService.generate(requestId, correlationId);
			}
			return current(requestId);
		}
		RevocationGateway.Result result;
		try {
			result = revocation.revoke(new RevocationGateway.Command(dispatch.digitalCredentialUuid(),
					dispatch.statusListIndex(), dispatch.dni(), dispatch.idempotencyKey()));
		}
		catch (RuntimeException exception) {
			LOGGER.warn("Revocation provider outcome is unknown requestId={} operationId={}",
					requestId, dispatch.operationId(), exception);
			result = new RevocationGateway.Result(RevocationResult.OUTCOME_UNKNOWN, null,
					"PROVIDER_UNAVAILABLE", clock.instant());
		}
		RevocationGateway.Result finalResult = result;
		transactions.executeWithoutResult(status -> complete(dispatch, finalResult, correlationId));
		if (result.outcome() == RevocationResult.SUCCEEDED) {
			receiptService.generate(requestId, correlationId);
		}
		return current(requestId);
	}

	public RevocationExecutionResponse retryReceipt(Long requestId, String correlationId) {
		ensurePersistence();
		receiptService.generate(requestId, correlationId);
		return current(requestId);
	}

	public RevocationExecutionResponse current(Long requestId) {
		ensurePersistence();
		return transactions.execute(status -> snapshot(requestId));
	}

	public byte[] receiptDocument(Long requestId) {
		ensurePersistence();
		return receiptService.document(requestId);
	}

	private Dispatch prepare(Long requestId, String correlationId) {
		DigitalCredentialRevocationRequestEntity request = requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> notAllowed("Request not found"));
		RevocationOperationEntity existing = operations
				.findFirstByRequest_IdOrderByAttemptNumberDesc(requestId).orElse(null);
		if (existing != null && existing.getOperationStatus() != RevocationOperationStatus.PREPARED
				&& existing.getOperationStatus() != RevocationOperationStatus.SUBMITTED
				&& existing.getOperationStatus() != RevocationOperationStatus.OUTCOME_UNKNOWN) {
			return null;
		}
		if (request.getRequestStatus() != RevocationRequestStatus.CONFIRMED
				&& request.getRequestStatus() != RevocationRequestStatus.REVOCATION_IN_PROGRESS
				&& request.getRequestStatus() != RevocationRequestStatus.REVOCATION_OUTCOME_UNKNOWN) {
			return null;
		}
		RevocationRequestDigitalCredentialEntity selected = selected(requestId, true);
		if (selected.getStatusListIndex() == null) {
			throw notAllowed("La credencial histórica no tiene un índice oficial para conciliación");
		}
		Instant now = clock.instant();
		if (existing != null && existing.getOperationStatus() == RevocationOperationStatus.SUBMITTED
				&& existing.getUpdatedAt().plus(properties.getStaleSubmissionThreshold()).isAfter(now)) {
			return null;
		}
		RevocationOperationEntity operation = existing;
		if (operation == null) {
			String key = "revocation-request-" + requestId;
			operation = operations.saveAndFlush(new RevocationOperationEntity(
					request, key, 1, now, correlationId));
			audit.save(new RevocationAuditEventEntity(request,
					RevocationAuditEventType.REVOCATION_PREPARED,
					request.getRequestStatus(), RevocationRequestStatus.REVOCATION_IN_PROGRESS,
					null, correlationId, AuditEventOrigin.BACKEND, now));
		}
		if (operation.getOperationStatus() == RevocationOperationStatus.PREPARED) {
			operation.markSubmitted(now, null);
			audit.save(new RevocationAuditEventEntity(request,
					RevocationAuditEventType.REVOCATION_SUBMITTED,
					RevocationRequestStatus.REVOCATION_IN_PROGRESS,
					RevocationRequestStatus.REVOCATION_IN_PROGRESS, null,
					correlationId, AuditEventOrigin.EXTERNAL_PROVIDER, now));
		}
		request.transitionTo(RevocationRequestStatus.REVOCATION_IN_PROGRESS, null);
		return new Dispatch(operation.getId(), operation.getIdempotencyKey(),
				selected.getDigitalCredentialUuid(), selected.getStatusListIndex(), request.getDni(), requestId);
	}

	private void complete(Dispatch dispatch, RevocationGateway.Result result, String correlationId) {
		DigitalCredentialRevocationRequestEntity request = requests.findByIdForUpdate(dispatch.requestId())
				.orElseThrow(() -> notAllowed("Request not found"));
		RevocationOperationEntity operation = operations.findById(dispatch.operationId())
				.orElseThrow(() -> notAllowed("Revocation operation not found"));
		if (operation.getOperationStatus() != RevocationOperationStatus.PREPARED
				&& operation.getOperationStatus() != RevocationOperationStatus.SUBMITTED
				&& operation.getOperationStatus() != RevocationOperationStatus.OUTCOME_UNKNOWN) return;

		Instant completed = result.respondedAt() == null ? Instant.now() : result.respondedAt();
		RevocationOperationStatus operationStatus = RevocationOperationStatus.valueOf(result.outcome().name());
		operation.complete(operationStatus, result.outcome(), completed, completed,
				result.externalReference(), result.errorCode(), result.providerCredentialStatus());
		selected(dispatch.requestId(), true).applyAtomicOutcome(result.outcome(), completed,
				result.providerCredentialStatus());
		RevocationRequestStatus previous = request.getRequestStatus();
		RevocationRequestStatus next = switch (result.outcome()) {
			case SUCCEEDED -> RevocationRequestStatus.REVOCATION_SUCCEEDED;
			case FAILED -> RevocationRequestStatus.REVOCATION_FAILED;
			case OUTCOME_UNKNOWN -> RevocationRequestStatus.REVOCATION_OUTCOME_UNKNOWN;
		};
		RevocationFinalOutcome outcome = switch (result.outcome()) {
			case SUCCEEDED -> RevocationFinalOutcome.REVOCATION_SUCCEEDED;
			case FAILED -> RevocationFinalOutcome.REVOCATION_FAILED;
			case OUTCOME_UNKNOWN -> RevocationFinalOutcome.OUTCOME_UNKNOWN;
		};
		request.transitionTo(next, outcome);
		audit.save(new RevocationAuditEventEntity(request,
				result.outcome() == RevocationResult.OUTCOME_UNKNOWN
						? RevocationAuditEventType.OUTCOME_UNKNOWN
						: RevocationAuditEventType.REVOCATION_CONFIRMED,
				previous, next, result.outcome().name(), correlationId,
				AuditEventOrigin.EXTERNAL_PROVIDER, completed));
	}

	private RevocationExecutionResponse snapshot(Long requestId) {
		DigitalCredentialRevocationRequestEntity request = requireRequest(requestId);
		RevocationRequestDigitalCredentialEntity selected = selected(requestId, false);
		RevocationOperationEntity operation = operations
				.findFirstByRequest_IdOrderByAttemptNumberDesc(requestId).orElse(null);
		RevocationReceiptService.Snapshot receipt = receiptService.snapshot(requestId);
		Instant serverTime = clock.instant();
		RevocationExecutionState state = switch (request.getRequestStatus()) {
			case RECEIPT_AVAILABLE -> RevocationExecutionState.SUCCEEDED;
			case REVOCATION_FAILED, FAILED -> RevocationExecutionState.FAILED;
			case REVOCATION_OUTCOME_UNKNOWN, OUTCOME_UNKNOWN -> RevocationExecutionState.OUTCOME_UNKNOWN;
			case REVOCATION_SUCCEEDED, COMPLETED -> receipt != null
					&& receipt.status() == ReceiptGenerationStatus.FAILED
							? RevocationExecutionState.RECEIPT_FAILED
							: RevocationExecutionState.PROCESSING;
			default -> RevocationExecutionState.PROCESSING;
		};
		RevocationExecutionResponse.Receipt receiptResponse = receipt == null ? null
				: new RevocationExecutionResponse.Receipt(receipt.code(),
						receipt.status(), receipt.availableAt(), receipt.downloadAvailable());
		RevocationExecutionResponse.Processing processing = processing(
				state, request, operation, receipt, serverTime);
		return new RevocationExecutionResponse(state, request.getRequestStatus(),
				maskDni(request.getDni()), verifiedFirstName(requestId),
				new RevocationExecutionResponse.DigitalCredential(requireOfficialIndex(selected),
						selected.getEmissionCreatedAt()),
				RevocationReasonCatalog.label(request.getReasonCode()), request.getOtherReason(),
				request.getConfirmedAt(), operation == null ? null : operation.getCompletedAt(),
				processing,
				receiptResponse);
	}

	private String verifiedFirstName(Long requestId) {
		return verifications.findFirstByRequest_IdOrderByAttemptNumberDesc(requestId)
				.filter(verification -> verification.getVerificationStatus() == IdentityVerificationStatus.VERIFIED)
				.filter(verification -> verification.getDniMatchResult() == IdentityMatchResult.MATCH)
				.map(IdentityVerificationEntity::getVerifiedFirstName)
				.filter(name -> !name.isBlank())
				.orElse(null);
	}

	private RevocationExecutionResponse.Processing processing(
			RevocationExecutionState state,
			DigitalCredentialRevocationRequestEntity request,
			RevocationOperationEntity operation,
			RevocationReceiptService.Snapshot receipt,
			Instant serverTime) {
		if (state != RevocationExecutionState.PROCESSING) return null;
		if (operation == null || operation.getCompletedAt() == null || !operation.isSucceeded()) {
			Instant startedAt = operation != null && operation.getSubmittedAt() != null
					? operation.getSubmittedAt()
					: request.getConfirmedAt();
			return startedAt == null ? null : new RevocationExecutionResponse.Processing(
					RevocationProcessingPhase.SUBMITTING, startedAt, null, serverTime);
		}
		Instant readyAt = operation.getCompletedAt().plus(properties.getPropagationDelay());
		RevocationProcessingPhase phase = serverTime.isBefore(readyAt)
				&& (receipt == null || receipt.status() == ReceiptGenerationStatus.PENDING)
						? RevocationProcessingPhase.PROPAGATING
						: RevocationProcessingPhase.GENERATING;
		return new RevocationExecutionResponse.Processing(
				phase, operation.getCompletedAt(), readyAt, serverTime);
	}

	private DigitalCredentialRevocationRequestEntity requireRequest(Long requestId) {
		return requests.findById(requestId).orElseThrow(() -> notAllowed("Request not found"));
	}

	private RevocationRequestDigitalCredentialEntity selected(Long requestId, boolean lock) {
		List<RevocationRequestDigitalCredentialEntity> items = lock
				? digitalCredentials.findByRequestIdForUpdate(requestId)
				: digitalCredentials.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(requestId);
		List<RevocationRequestDigitalCredentialEntity> selected = lock
				? items.stream().filter(RevocationRequestDigitalCredentialEntity::isSelected).toList()
				: items;
		if (selected.size() != 1) throw notAllowed("Exactly one selected digitalCredential is required");
		return selected.getFirst();
	}

	private static String maskDni(String dni) { return "******" + dni.substring(dni.length() - 2); }
	private static int requireOfficialIndex(RevocationRequestDigitalCredentialEntity selected) {
		if (selected.getStatusListIndex() == null) throw notAllowed(
				"La credencial histórica no tiene un índice oficial para conciliación");
		return selected.getStatusListIndex();
	}
	private static RevocationConfirmationException notAllowed(String message) {
		return new RevocationConfirmationException(
				RevocationConfirmationException.Reason.NOT_ALLOWED, message);
	}

	private void ensurePersistence() {
		if (requests == null || digitalCredentials == null || operations == null
				|| audit == null || verifications == null || transactions == null) {
			throw notAllowed("Revocation execution is unavailable");
		}
	}

	private void ensureRevocationAvailable() {
		if (!revocation.isAvailable()) {
			throw new RevocationConfirmationException(
					RevocationConfirmationException.Reason.DEPENDENCY_UNAVAILABLE,
					"Revocation integration is unavailable");
		}
	}

	private record Dispatch(Long operationId, String idempotencyKey,
			String digitalCredentialUuid, int statusListIndex, String dni, Long requestId) { }
}
