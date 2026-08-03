package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import java.util.List;
import java.util.LinkedHashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.ReceiptGenerationStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReceiptRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationRepository;

@Component
@ConditionalOnProperty(prefix = "app.receipt", name = "processing-enabled", havingValue = "true")
public class RevocationReceiptProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(RevocationReceiptProcessor.class);
	private static final int BATCH_SIZE = 50;
	private static final List<ReceiptGenerationStatus> CANDIDATE_STATUSES = List.of(
			ReceiptGenerationStatus.PENDING, ReceiptGenerationStatus.GENERATING);

	private final RevocationReceiptRepository receipts;
	private final RevocationOperationRepository operations;
	private final RevocationReceiptService service;

	public RevocationReceiptProcessor(RevocationReceiptRepository receipts,
			RevocationOperationRepository operations,
			RevocationReceiptService service) {
		this.receipts = receipts;
		this.operations = operations;
		this.service = service;
	}

	@Scheduled(fixedDelayString = "#{@receiptProperties.getProcessingInterval().toMillis()}")
	public void processPendingReceipts() {
		PageRequest batch = PageRequest.of(0, BATCH_SIZE);
		LinkedHashSet<Long> requestIds = new LinkedHashSet<>(
				operations.findSuccessfulRequestIdsWithoutReceipt(batch));
		requestIds.addAll(receipts.findGenerationCandidateRequestIds(CANDIDATE_STATUSES, batch));
		for (Long requestId : requestIds) {
			try {
				service.generate(requestId, "receipt-background-processor");
			}
			catch (RuntimeException exception) {
				LOGGER.warn("Receipt background processing failed requestId={}", requestId, exception);
			}
		}
	}
}
