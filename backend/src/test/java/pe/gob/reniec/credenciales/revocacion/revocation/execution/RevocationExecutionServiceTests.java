package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationConfirmationException;
import pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationConfirmationRequest;
import pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationConfirmationService;
import pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationConsentCatalog;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationAuditEventRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReasonCode;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestDigitalCredentialRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.*;

class RevocationExecutionServiceTests {

	@Test
	void exposesTheAuthoritativePropagationWindowWithoutCallingTheProviderAgain() {
		Instant completedAt = Instant.parse("2026-07-31T18:00:00Z");
		Instant now = completedAt.plusSeconds(30);
		DigitalCredentialRevocationRequestEntity request = new DigitalCredentialRevocationRequestEntity("73905791");
		ReflectionTestUtils.setField(request, "id", 7L);
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE);
		RevocationRequestDigitalCredentialEntity digitalCredential = new RevocationRequestDigitalCredentialEntity(
				request, 31, "DniPeruanoCredential", completedAt.minusSeconds(1_000),
				"11111111-1111-4111-8111-111111111111",
				DigitalCredentialAvailabilityStatus.AVAILABLE, null, 0, completedAt.minusSeconds(900));
		digitalCredential.select(completedAt.minusSeconds(100));
		request.confirmDecision(RevocationReasonCode.THEFT, null,
				completedAt.minusSeconds(100), RevocationConsentCatalog.VERSION);
		request.transitionTo(RevocationRequestStatus.REVOCATION_SUCCEEDED,
				RevocationFinalOutcome.REVOCATION_SUCCEEDED);
		RevocationOperationEntity operation = new RevocationOperationEntity(
				request, "revocation-request-7", 1, completedAt.minusSeconds(10), "correlation");
		operation.complete(RevocationOperationStatus.SUCCEEDED, RevocationResult.SUCCEEDED,
				completedAt, completedAt, "provider-ref", null);

		DigitalCredentialRevocationRequestRepository requests = mock(DigitalCredentialRevocationRequestRepository.class);
		RevocationRequestDigitalCredentialRepository digitalCredentials = mock(RevocationRequestDigitalCredentialRepository.class);
		RevocationOperationRepository operations = mock(RevocationOperationRepository.class);
		RevocationAuditEventRepository audit = mock(RevocationAuditEventRepository.class);
		IdentityVerificationRepository verifications = mock(IdentityVerificationRepository.class);
		RevocationGateway gateway = mock(RevocationGateway.class);
		RevocationReceiptService receipts = mock(RevocationReceiptService.class);
		when(requests.findById(7L)).thenReturn(Optional.of(request));
		when(digitalCredentials.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(7L))
				.thenReturn(List.of(digitalCredential));
		when(operations.findFirstByRequest_IdOrderByAttemptNumberDesc(7L)).thenReturn(Optional.of(operation));
		IdentityVerificationEntity identity = new IdentityVerificationEntity(
				request, 1, "ID_PERU", completedAt.minusSeconds(300), "identity-correlation");
		identity.finish(IdentityVerificationStatus.VERIFIED, IdentityMatchResult.MATCH,
				completedAt.minusSeconds(290), "identity-reference", null, null, null, "ANA");
		when(verifications.findFirstByRequest_IdOrderByAttemptNumberDesc(7L))
				.thenReturn(Optional.of(identity));
		when(receipts.snapshot(7L)).thenReturn(new RevocationReceiptService.Snapshot(
				"RV-2026-000007", ReceiptGenerationStatus.PENDING, null));
		RevocationProperties properties = new RevocationProperties();
		properties.setPropagationDelay(Duration.ofSeconds(60));

		RevocationExecutionService service = new RevocationExecutionService(
				mock(RevocationConfirmationService.class), provider(requests), provider(digitalCredentials),
				provider(operations), provider(audit), provider(verifications), gateway, properties, receipts,
				provider(Clock.fixed(now, ZoneOffset.UTC)), provider(transactionManager()));

		RevocationExecutionResponse response = service.current(7L);

		assertThat(response.state()).isEqualTo(RevocationExecutionState.PROCESSING);
		assertThat(response.firstName()).isEqualTo("ANA");
		assertThat(response.processing().phase()).isEqualTo(RevocationProcessingPhase.PROPAGATING);
		assertThat(response.processing().startedAt()).isEqualTo(completedAt);
		assertThat(response.processing().readyAt()).isEqualTo(completedAt.plusSeconds(60));
		assertThat(response.processing().serverTime()).isEqualTo(now);
		verifyNoInteractions(gateway);
	}

	@Test
	void rejectsUnavailableIntegrationBeforePersistingConfirmation() {
		RevocationConfirmationService confirmation = mock(RevocationConfirmationService.class);
		RevocationExecutionService service = new RevocationExecutionService(
				confirmation,
				provider(mock(DigitalCredentialRevocationRequestRepository.class)),
				provider(mock(RevocationRequestDigitalCredentialRepository.class)),
				provider(mock(RevocationOperationRepository.class)),
				provider(mock(RevocationAuditEventRepository.class)),
				provider(mock(IdentityVerificationRepository.class)),
				new DisabledRevocationGateway(),
				new RevocationProperties(),
				mock(RevocationReceiptService.class),
				provider(Clock.systemUTC()),
				provider(mock(PlatformTransactionManager.class)));
		RevocationConfirmationRequest command = new RevocationConfirmationRequest(
				"11111111-1111-4111-8111-111111111111",
				31, RevocationReasonCode.THEFT, null, true, RevocationConsentCatalog.VERSION);

		assertThatThrownBy(() -> service.confirmAndExecute(7L, command, "correlation"))
				.isInstanceOfSatisfying(RevocationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								RevocationConfirmationException.Reason.DEPENDENCY_UNAVAILABLE));
		verifyNoInteractions(confirmation);
	}

	@SuppressWarnings("unchecked")
	private static <T> ObjectProvider<T> provider(T value) {
		ObjectProvider<T> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(value);
		return provider;
	}

	private static PlatformTransactionManager transactionManager() {
		PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
		when(manager.getTransaction(org.mockito.ArgumentMatchers.any()))
				.thenAnswer(ignored -> new SimpleTransactionStatus());
		return manager;
	}
}
