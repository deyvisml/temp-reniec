package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReceiptRepository;

class RevocationReceiptProcessorTests {

	@Test
	void recoversSuccessfulOperationsWithoutReceiptAndPendingGenerationsWithoutDuplicates() {
		RevocationReceiptRepository receipts = mock(RevocationReceiptRepository.class);
		RevocationOperationRepository operations = mock(RevocationOperationRepository.class);
		RevocationReceiptService service = mock(RevocationReceiptService.class);
		when(operations.findSuccessfulRequestIdsWithoutReceipt(any()))
				.thenReturn(List.of(7L, 8L));
		when(receipts.findGenerationCandidateRequestIds(anyCollection(), any()))
				.thenReturn(List.of(8L, 9L));

		new RevocationReceiptProcessor(receipts, operations, service).processPendingReceipts();

		verify(service).generate(7L, "receipt-background-processor");
		verify(service, times(1)).generate(8L, "receipt-background-processor");
		verify(service).generate(9L, "receipt-background-processor");
	}
}
