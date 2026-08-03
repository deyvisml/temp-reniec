package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationAuditEventRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialAvailabilityStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationFinalOutcome;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReasonCode;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReceiptEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReceiptRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestDigitalCredentialEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestDigitalCredentialRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.CurrentAvailabilityResult;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationResult;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityMatchResult;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationStatus;

class RevocationReceiptServiceTests {

	@Test
	void keepsTheReceiptPendingUntilThePropagationDelayExpires() {
		Instant now = Instant.parse("2026-07-31T18:00:30Z");
		DigitalCredentialRevocationRequestEntity request = successfulRequest(now);
		RevocationRequestDigitalCredentialEntity digitalCredential = selectedCredential(request, now);
		markSuccessful(request, now);
		RevocationOperationEntity operation = successfulOperation(request,
				Instant.parse("2026-07-31T18:00:00Z"));
		RevocationReceiptEntity receipt = new RevocationReceiptEntity(
				request, operation, "RV-2026-000007");

		DigitalCredentialRevocationRequestRepository requests =
				mock(DigitalCredentialRevocationRequestRepository.class);
		RevocationRequestDigitalCredentialRepository digitalCredentials =
				mock(RevocationRequestDigitalCredentialRepository.class);
		RevocationOperationRepository operations = mock(RevocationOperationRepository.class);
		RevocationReceiptRepository receipts = mock(RevocationReceiptRepository.class);
		RevocationAuditEventRepository audit = mock(RevocationAuditEventRepository.class);
		IdentityVerificationRepository verifications = mock(IdentityVerificationRepository.class);
		ReceiptStorage storage = mock(ReceiptStorage.class);
		RevocationReceiptPdfRenderer pdf = mock(RevocationReceiptPdfRenderer.class);
		ReceiptProperties properties = new ReceiptProperties();
		RevocationProperties revocationProperties = new RevocationProperties();
		revocationProperties.setPropagationDelay(Duration.ofSeconds(60));

		when(requests.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
		when(operations.findFirstByRequest_IdOrderByAttemptNumberDesc(7L))
				.thenReturn(Optional.of(operation));
		when(receipts.findFirstByRequest_IdOrderByCreatedAtDesc(7L))
				.thenReturn(Optional.of(receipt));
		when(digitalCredentials.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(7L))
				.thenReturn(List.of(digitalCredential));

		RevocationReceiptService service = new RevocationReceiptService(
				provider(requests), provider(digitalCredentials), provider(operations),
				provider(receipts), provider(audit), provider(verifications), storage, pdf, properties,
				revocationProperties, provider(Clock.fixed(now, ZoneOffset.UTC)),
				provider(transactionManager()));

		service.generate(7L, "propagation-correlation");

		assertThat(receipt.getGenerationStatus().name()).isEqualTo("PENDING");
		verifyNoInteractions(pdf, storage);
	}

	@Test
	void resumesAnAbandonedGenerationAfterTheConfiguredThreshold() throws Exception {
		Instant now = Instant.now();
		DigitalCredentialRevocationRequestEntity request = new DigitalCredentialRevocationRequestEntity("73905791");
		ReflectionTestUtils.setField(request, "id", 7L);
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE);
		RevocationRequestDigitalCredentialEntity digitalCredential = new RevocationRequestDigitalCredentialEntity(
				request, 31, "DniPeruanoCredential", now.minusSeconds(100),
				"11111111-1111-4111-8111-111111111111",
				DigitalCredentialAvailabilityStatus.AVAILABLE, null, 0, now.minusSeconds(90));
		digitalCredential.select(now.minusSeconds(80));
		request.confirmDecision(RevocationReasonCode.OTHER,
				"El dispositivo anterior dejó de estar bajo mi control.",
				now.minusSeconds(80), "REVOCACION_CREDENCIALES_DIGITALES_V1");
		request.transitionTo(RevocationRequestStatus.REVOCATION_SUCCEEDED,
				RevocationFinalOutcome.REVOCATION_SUCCEEDED);

		RevocationOperationEntity operation = new RevocationOperationEntity(
				request, "cancel-request-7", 1, now.minusSeconds(70), "correlation");
		operation.complete(RevocationOperationStatus.SUCCEEDED, RevocationResult.SUCCEEDED,
				now.minusSeconds(60), now.minusSeconds(60), "provider-ref", null);
		RevocationReceiptEntity receipt = new RevocationReceiptEntity(
				request, operation, "RV-2026-000007");
		ReflectionTestUtils.setField(receipt, "id", 9L);
		receipt.markGenerating();
		ReflectionTestUtils.setField(receipt, "updatedAt", now.minusSeconds(30));

		DigitalCredentialRevocationRequestRepository requests =
				mock(DigitalCredentialRevocationRequestRepository.class);
		RevocationRequestDigitalCredentialRepository digitalCredentials =
				mock(RevocationRequestDigitalCredentialRepository.class);
		RevocationOperationRepository operations = mock(RevocationOperationRepository.class);
		RevocationReceiptRepository receipts = mock(RevocationReceiptRepository.class);
		RevocationAuditEventRepository audit = mock(RevocationAuditEventRepository.class);
		IdentityVerificationRepository verifications = mock(IdentityVerificationRepository.class);
		ReceiptStorage storage = mock(ReceiptStorage.class);
		RevocationReceiptPdfRenderer pdf = mock(RevocationReceiptPdfRenderer.class);
		PlatformTransactionManager transactionManager = transactionManager();
		ReceiptProperties properties = new ReceiptProperties();
		properties.setStaleGenerationThreshold(Duration.ofSeconds(1));
		RevocationProperties revocationProperties = new RevocationProperties();
		revocationProperties.setPropagationDelay(Duration.ofSeconds(60));

		when(requests.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
		when(operations.findFirstByRequest_IdOrderByAttemptNumberDesc(7L))
				.thenReturn(Optional.of(operation));
		when(receipts.findFirstByRequest_IdOrderByCreatedAtDesc(7L))
				.thenReturn(Optional.of(receipt));
		when(digitalCredentials.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(7L))
				.thenReturn(List.of(digitalCredential));
		when(receipts.findById(9L)).thenReturn(Optional.of(receipt));
		when(verifications.findFirstByRequest_IdOrderByAttemptNumberDesc(7L))
				.thenReturn(Optional.of(verifiedIdentity(request, now)));
		when(pdf.render(any())).thenReturn(new byte[] { 1, 2, 3 });
		when(storage.store(eq("RV-2026-000007"), any(byte[].class)))
				.thenReturn("RV-2026-000007.pdf");

		RevocationReceiptService service = new RevocationReceiptService(
				provider(requests), provider(digitalCredentials), provider(operations),
				provider(receipts), provider(audit), provider(verifications), storage, pdf, properties,
				revocationProperties, provider(Clock.fixed(now, ZoneOffset.UTC)),
				provider(transactionManager));

		service.generate(7L, "retry-correlation");

		assertThat(receipt.getGenerationStatus().name()).isEqualTo("AVAILABLE");
		assertThat(receipt.getStorageReference()).isEqualTo("RV-2026-000007.pdf");
		ArgumentCaptor<RevocationReceiptPdfRenderer.Data> pdfData =
				ArgumentCaptor.forClass(RevocationReceiptPdfRenderer.Data.class);
		verify(pdf).render(pdfData.capture());
		assertThat(pdfData.getValue().dni()).isEqualTo("73905791");
		verify(storage).store(eq("RV-2026-000007"), any(byte[].class));
	}

	@Test
	void failsSafelyWhenAHistoricalSuccessfulRevocationHasNoVerifiedName() {
		Instant now = Instant.parse("2026-07-31T18:02:00Z");
		DigitalCredentialRevocationRequestEntity request = successfulRequest(now);
		RevocationRequestDigitalCredentialEntity digitalCredential = selectedCredential(request, now);
		markSuccessful(request, now);
		RevocationOperationEntity operation = successfulOperation(request,
				Instant.parse("2026-07-31T18:00:00Z"));
		RevocationReceiptEntity receipt = new RevocationReceiptEntity(
				request, operation, "RV-2026-000007");

		DigitalCredentialRevocationRequestRepository requests = mock(DigitalCredentialRevocationRequestRepository.class);
		RevocationRequestDigitalCredentialRepository digitalCredentials = mock(RevocationRequestDigitalCredentialRepository.class);
		RevocationOperationRepository operations = mock(RevocationOperationRepository.class);
		RevocationReceiptRepository receipts = mock(RevocationReceiptRepository.class);
		RevocationAuditEventRepository audit = mock(RevocationAuditEventRepository.class);
		IdentityVerificationRepository verifications = mock(IdentityVerificationRepository.class);
		ReceiptStorage storage = mock(ReceiptStorage.class);
		RevocationReceiptPdfRenderer pdf = mock(RevocationReceiptPdfRenderer.class);

		when(requests.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
		when(operations.findFirstByRequest_IdOrderByAttemptNumberDesc(7L)).thenReturn(Optional.of(operation));
		when(receipts.findFirstByRequest_IdOrderByCreatedAtDesc(7L)).thenReturn(Optional.of(receipt));
		when(digitalCredentials.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(7L))
				.thenReturn(List.of(digitalCredential));
		when(verifications.findFirstByRequest_IdOrderByAttemptNumberDesc(7L)).thenReturn(Optional.empty());

		RevocationProperties revocationProperties = new RevocationProperties();
		revocationProperties.setPropagationDelay(Duration.ofSeconds(60));
		RevocationReceiptService service = new RevocationReceiptService(
				provider(requests), provider(digitalCredentials), provider(operations),
				provider(receipts), provider(audit), provider(verifications), storage, pdf,
				new ReceiptProperties(), revocationProperties, provider(Clock.fixed(now, ZoneOffset.UTC)),
				provider(transactionManager()));

		service.generate(7L, "missing-name-correlation");

		assertThat(receipt.getGenerationStatus().name()).isEqualTo("FAILED");
		assertThat(receipt.getErrorCode()).isEqualTo("IDENTITY_NAME_UNAVAILABLE");
		verifyNoInteractions(pdf, storage);
		verify(audit).save(any());
	}

	private static DigitalCredentialRevocationRequestEntity successfulRequest(Instant now) {
		DigitalCredentialRevocationRequestEntity request = new DigitalCredentialRevocationRequestEntity("73905791");
		ReflectionTestUtils.setField(request, "id", 7L);
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE);
		return request;
	}

	private static void markSuccessful(DigitalCredentialRevocationRequestEntity request, Instant now) {
		request.confirmDecision(RevocationReasonCode.OTHER,
				"El dispositivo anterior dejó de estar bajo mi control.",
				now.minusSeconds(90), "REVOCACION_CREDENCIALES_DIGITALES_V1");
		request.transitionTo(RevocationRequestStatus.REVOCATION_SUCCEEDED,
				RevocationFinalOutcome.REVOCATION_SUCCEEDED);
	}

	private static RevocationRequestDigitalCredentialEntity selectedCredential(
			DigitalCredentialRevocationRequestEntity request, Instant now) {
		RevocationRequestDigitalCredentialEntity digitalCredential = new RevocationRequestDigitalCredentialEntity(
				request, 31, "DniPeruanoCredential", now.minusSeconds(200),
				"11111111-1111-4111-8111-111111111111",
				DigitalCredentialAvailabilityStatus.AVAILABLE, null, 0, now.minusSeconds(190));
		digitalCredential.select(now.minusSeconds(100));
		return digitalCredential;
	}

	private static RevocationOperationEntity successfulOperation(
			DigitalCredentialRevocationRequestEntity request, Instant completedAt) {
		RevocationOperationEntity operation = new RevocationOperationEntity(
				request, "revocation-request-7", 1, completedAt.minusSeconds(10), "correlation");
		operation.complete(RevocationOperationStatus.SUCCEEDED, RevocationResult.SUCCEEDED,
				completedAt, completedAt, "provider-ref", null);
		return operation;
	}

	private static IdentityVerificationEntity verifiedIdentity(
			DigitalCredentialRevocationRequestEntity request, Instant now) {
		IdentityVerificationEntity verification = new IdentityVerificationEntity(
				request, 1, "ID_PERU", now.minusSeconds(300), "identity-correlation");
		verification.finish(IdentityVerificationStatus.VERIFIED, IdentityMatchResult.MATCH,
				now.minusSeconds(290), "identity-reference", null, null, null, "ANA");
		return verification;
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
