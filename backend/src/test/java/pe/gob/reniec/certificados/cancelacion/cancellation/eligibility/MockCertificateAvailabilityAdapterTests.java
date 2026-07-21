package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class MockCertificateAvailabilityAdapterTests {

	private final MockCertificateAvailabilityAdapter adapter =
			new MockCertificateAvailabilityAdapter(Duration.ofMillis(1));

	@Test
	void mapsEveryDocumentedFixtureDeterministically() {
		assertOutcome("00000001", AvailabilityOutcome.AVAILABLE);
		assertOutcome("00000002", AvailabilityOutcome.NOT_AVAILABLE);
		assertOutcome("00000003", AvailabilityOutcome.UNAVAILABLE);
		assertOutcome("00000004", AvailabilityOutcome.INCONCLUSIVE);
		assertOutcome("00000005", AvailabilityOutcome.ERROR);
		assertOutcome("00000006", AvailabilityOutcome.UNAVAILABLE);
		assertOutcome("12345678", AvailabilityOutcome.NOT_AVAILABLE);
	}

	private void assertOutcome(String dni, AvailabilityOutcome expected) {
		assertThat(adapter.checkAvailability(dni).outcome()).isEqualTo(expected);
		assertThat(adapter.checkAvailability(dni).outcome()).isEqualTo(expected);
	}
}
