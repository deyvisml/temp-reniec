package pe.gob.reniec.certificados.cancelacion.cancellation.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;

import pe.gob.reniec.certificados.cancelacion.cancellation.confirmation.CancellationConfirmationException;
import pe.gob.reniec.certificados.cancelacion.cancellation.confirmation.CancellationConfirmationRequest;
import pe.gob.reniec.certificados.cancelacion.cancellation.confirmation.CancellationConfirmationService;
import pe.gob.reniec.certificados.cancelacion.cancellation.confirmation.CancellationConsentCatalog;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationAuditEventRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReasonCode;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestCertificateRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationRepository;

class CancellationExecutionServiceTests {

	@Test
	void rejectsUnavailableIntegrationBeforePersistingConfirmation() {
		CancellationConfirmationService confirmation = mock(CancellationConfirmationService.class);
		CancellationExecutionService service = new CancellationExecutionService(
				confirmation,
				provider(mock(CertificateCancellationRequestRepository.class)),
				provider(mock(CancellationRequestCertificateRepository.class)),
				provider(mock(RevocationOperationRepository.class)),
				provider(mock(CancellationAuditEventRepository.class)),
				new DisabledRevocationGateway(),
				new RevocationProperties(),
				mock(CancellationReceiptService.class),
				provider(mock(PlatformTransactionManager.class)));
		CancellationConfirmationRequest command = new CancellationConfirmationRequest(
				"11111111-1111-4111-8111-111111111111",
				CancellationReasonCode.THEFT, null, true, CancellationConsentCatalog.VERSION);

		assertThatThrownBy(() -> service.confirmAndExecute(7L, command, "correlation"))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								CancellationConfirmationException.Reason.DEPENDENCY_UNAVAILABLE));
		verifyNoInteractions(confirmation);
	}

	@SuppressWarnings("unchecked")
	private static <T> ObjectProvider<T> provider(T value) {
		ObjectProvider<T> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(value);
		return provider;
	}
}
