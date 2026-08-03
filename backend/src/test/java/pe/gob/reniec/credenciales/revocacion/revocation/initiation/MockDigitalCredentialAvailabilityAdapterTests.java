package pe.gob.reniec.credenciales.revocacion.revocation.initiation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class MockDigitalCredentialAvailabilityAdapterTests {

	private final MockDigitalCredentialAvailabilityAdapter adapter =
			new MockDigitalCredentialAvailabilityAdapter(Duration.ofMillis(1));

	@Test
	void mapsNormalDnisToAvailableAndEverySpecialFixtureDeterministically() {
		assertOutcome("00000001", AvailabilityOutcome.AVAILABLE);
		assertOutcome("00000002", AvailabilityOutcome.NOT_AVAILABLE);
		assertOutcome("00000003", AvailabilityOutcome.UNAVAILABLE);
		assertOutcome("00000004", AvailabilityOutcome.INCONCLUSIVE);
		assertOutcome("00000005", AvailabilityOutcome.ERROR);
		assertOutcome("00000006", AvailabilityOutcome.UNAVAILABLE);
		assertOutcome("12345678", AvailabilityOutcome.AVAILABLE);
		assertOutcome("87654321", AvailabilityOutcome.AVAILABLE);
	}

	@Test
	void keepsSuccessReferencesSeparateFromFailureCodes() {
		AvailabilityResult available = adapter.checkAvailability("00000001");
		AvailabilityResult unavailable = adapter.checkAvailability("00000003");

		assertThat(available.externalReference()).isEqualTo("mock-availability");
		assertThat(available.technicalCode()).isNull();
		assertThat(unavailable.externalReference()).isNull();
		assertThat(unavailable.technicalCode()).isEqualTo("MOCK_UNAVAILABLE");
	}

	@Test
	void rejectsInvalidProviderMetadataAtThePortBoundary() {
		assertThatThrownBy(() -> new AvailabilityResult(AvailabilityOutcome.ERROR, null, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new AvailabilityResult(
				AvailabilityOutcome.AVAILABLE, "á", null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new AvailabilityResult(
				AvailabilityOutcome.ERROR, null, "X".repeat(65)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private void assertOutcome(String dni, AvailabilityOutcome expected) {
		assertThat(adapter.checkAvailability(dni).outcome()).isEqualTo(expected);
		assertThat(adapter.checkAvailability(dni).outcome()).isEqualTo(expected);
	}
}
