package pe.gob.reniec.credenciales.revocacion.revocation.initiation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.gob.reniec.credenciales.revocacion.revocation.initiation.AvailabilityPersistenceCoordinator.AvailabilityPreparation;
import pe.gob.reniec.credenciales.revocacion.revocation.initiation.antibot.AntiBotVerificationPort;
import pe.gob.reniec.credenciales.revocacion.revocation.initiation.antibot.RecaptchaFailure;
import pe.gob.reniec.credenciales.revocacion.revocation.initiation.antibot.RecaptchaVerificationException;

@ExtendWith(MockitoExtension.class)
class RevocationRequestInitiationServiceTests {

	@Mock AvailabilityPersistenceCoordinator persistence;
	@Mock DigitalCredentialAvailabilityPort availabilityPort;
	@Mock AntiBotVerificationPort antiBotVerificationPort;

	private RevocationRequestInitiationService service;

	@AfterEach
	void closeExecutor() {
		if (service != null) service.closeExecutor();
	}

	@Test
	void convertsANullProviderResponseIntoAControlledFailedAttempt() {
		AvailabilityPreparation preparation = new AvailabilityPreparation(1L, 2L);
		when(persistence.prepare("00000001", "unit-correlation")).thenReturn(preparation);
		when(availabilityPort.checkAvailability("00000001")).thenReturn(null);
		service = new RevocationRequestInitiationService(persistence, availabilityPort,
				antiBotVerificationPort, Duration.ofSeconds(1));

		assertThatThrownBy(() -> service.initiate("00000001", "valid-token", "unit-correlation"))
				.isInstanceOf(AvailabilityProviderException.class);

		ArgumentCaptor<AvailabilityResult> result = ArgumentCaptor.forClass(AvailabilityResult.class);
		verify(persistence).finalizeAttempt(org.mockito.ArgumentMatchers.eq(preparation), result.capture());
		assertThat(result.getValue().outcome()).isEqualTo(AvailabilityOutcome.ERROR);
		assertThat(result.getValue().technicalCode()).isEqualTo("AVAILABILITY_INVALID_RESPONSE");
	}

	@Test
	void verifiesCaptchaBeforePersistenceAndAvailability() {
		AvailabilityPreparation preparation = new AvailabilityPreparation(1L, 2L);
		AvailabilityResult available = new AvailabilityResult(AvailabilityOutcome.AVAILABLE, "provider-ref", null);
		RevocationRequestResponse expected = org.mockito.Mockito.mock(RevocationRequestResponse.class);
		when(persistence.prepare("00000001", "unit-correlation")).thenReturn(preparation);
		when(availabilityPort.checkAvailability("00000001")).thenReturn(available);
		when(persistence.finalizeAttempt(preparation, available)).thenReturn(expected);
		service = new RevocationRequestInitiationService(persistence, availabilityPort,
				antiBotVerificationPort, Duration.ofSeconds(1));

		assertThat(service.initiate("00000001", "valid-token", "unit-correlation")).isSameAs(expected);

		var ordered = inOrder(antiBotVerificationPort, persistence, availabilityPort);
		ordered.verify(antiBotVerificationPort).verify("valid-token");
		ordered.verify(persistence).prepare("00000001", "unit-correlation");
		ordered.verify(availabilityPort).checkAvailability("00000001");
	}

	@Test
	void captchaFailurePerformsNoProtectedOperation() {
		org.mockito.Mockito.doThrow(new RecaptchaVerificationException(RecaptchaFailure.REJECTED))
				.when(antiBotVerificationPort).verify("rejected-token");
		service = new RevocationRequestInitiationService(persistence, availabilityPort,
				antiBotVerificationPort, Duration.ofSeconds(1));

		assertThatThrownBy(() -> service.initiate("00000001", "rejected-token", "unit-correlation"))
				.isInstanceOf(RecaptchaVerificationException.class);
		verify(persistence, never()).prepare(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
		verify(availabilityPort, never()).checkAvailability(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void rejectsNonPositiveTimeoutConfiguration() {
		assertThatThrownBy(() -> new RevocationRequestInitiationService(
				persistence, availabilityPort, antiBotVerificationPort, Duration.ZERO))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("app.availability.timeout");
	}
}
