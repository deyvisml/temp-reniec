package pe.gob.reniec.certificados.cancelacion.cancellation.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationAuditEventRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationFinalOutcome;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReasonCode;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReceiptEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReceiptRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestCertificateEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestCertificateRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CurrentAvailabilityResult;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationResult;

class CancellationReceiptServiceTests {

	@Test
	void resumesAnAbandonedGenerationAfterTheConfiguredThreshold() throws Exception {
		Instant now = Instant.now();
		CertificateCancellationRequestEntity request = new CertificateCancellationRequestEntity("73905791");
		ReflectionTestUtils.setField(request, "id", 7L);
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				CancellationRequestStatus.CERTIFICATES_AVAILABLE);
		CancellationRequestCertificateEntity certificate = new CancellationRequestCertificateEntity(
				request, "0000123456", now.minusSeconds(100),
				"11111111-1111-4111-8111-111111111111", now.minusSeconds(90));
		certificate.select(now.minusSeconds(80));
		request.confirmDecision(CancellationReasonCode.OTHER,
				"El dispositivo anterior dejó de estar bajo mi control.",
				now.minusSeconds(80), "CANCELACION_CERTIFICADOS_V1");
		request.transitionTo(CancellationRequestStatus.REVOCATION_SUCCEEDED,
				CancellationFinalOutcome.REVOCATION_SUCCEEDED);

		RevocationOperationEntity operation = new RevocationOperationEntity(
				request, "cancel-request-7", 1, now.minusSeconds(70), "correlation");
		operation.complete(RevocationOperationStatus.SUCCEEDED, RevocationResult.SUCCEEDED,
				now.minusSeconds(60), now.minusSeconds(60), "provider-ref", null);
		CancellationReceiptEntity receipt = new CancellationReceiptEntity(
				request, operation, "CD-2026-000007");
		ReflectionTestUtils.setField(receipt, "id", 9L);
		receipt.markGenerating();
		ReflectionTestUtils.setField(receipt, "updatedAt", now.minusSeconds(30));

		CertificateCancellationRequestRepository requests =
				mock(CertificateCancellationRequestRepository.class);
		CancellationRequestCertificateRepository certificates =
				mock(CancellationRequestCertificateRepository.class);
		RevocationOperationRepository operations = mock(RevocationOperationRepository.class);
		CancellationReceiptRepository receipts = mock(CancellationReceiptRepository.class);
		CancellationAuditEventRepository audit = mock(CancellationAuditEventRepository.class);
		ReceiptStorage storage = mock(ReceiptStorage.class);
		CancellationReceiptPdfRenderer pdf = mock(CancellationReceiptPdfRenderer.class);
		PlatformTransactionManager transactionManager = transactionManager();
		ReceiptProperties properties = new ReceiptProperties();
		properties.setStaleGenerationThreshold(Duration.ofSeconds(1));

		when(requests.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
		when(operations.findFirstByRequest_IdOrderByAttemptNumberDesc(7L))
				.thenReturn(Optional.of(operation));
		when(receipts.findFirstByRequest_IdOrderByCreatedAtDesc(7L))
				.thenReturn(Optional.of(receipt));
		when(certificates.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(7L))
				.thenReturn(List.of(certificate));
		when(receipts.findById(9L)).thenReturn(Optional.of(receipt));
		when(pdf.render(any())).thenReturn(new byte[] { 1, 2, 3 });
		when(storage.store(eq("CD-2026-000007"), any(byte[].class)))
				.thenReturn("CD-2026-000007.pdf");

		CancellationReceiptService service = new CancellationReceiptService(
				provider(requests), provider(certificates), provider(operations),
				provider(receipts), provider(audit), storage, pdf, properties,
				provider(transactionManager));

		service.generate(7L, "retry-correlation");

		assertThat(receipt.getGenerationStatus().name()).isEqualTo("AVAILABLE");
		assertThat(receipt.getStorageReference()).isEqualTo("CD-2026-000007.pdf");
		verify(storage).store(eq("CD-2026-000007"), any(byte[].class));
	}

	private static PlatformTransactionManager transactionManager() {
		PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
		when(manager.getTransaction(any())).thenAnswer(ignored -> new SimpleTransactionStatus());
		return manager;
	}

	@SuppressWarnings("unchecked")
	private static <T> ObjectProvider<T> provider(T value) {
		ObjectProvider<T> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(value);
		return provider;
	}
}
