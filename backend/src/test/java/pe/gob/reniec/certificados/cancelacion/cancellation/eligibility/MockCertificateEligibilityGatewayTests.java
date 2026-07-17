package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class MockCertificateEligibilityGatewayTests {

	private final MockCertificateEligibilityGateway gateway =
			new MockCertificateEligibilityGateway(Duration.ofMillis(1));

	@Test
	void mapsEveryDocumentedFixtureDeterministically() {
		assertOutcome("00000001", EligibilityOutcome.ELIGIBLE);
		assertOutcome("00000002", EligibilityOutcome.NOT_ELIGIBLE);
		assertOutcome("00000003", EligibilityOutcome.UNAVAILABLE);
		assertOutcome("00000004", EligibilityOutcome.INCONCLUSIVE);
		assertOutcome("00000005", EligibilityOutcome.ERROR);
		assertOutcome("00000006", EligibilityOutcome.UNAVAILABLE);
		assertOutcome("12345678", EligibilityOutcome.NOT_ELIGIBLE);
	}

	private void assertOutcome(String dni, EligibilityOutcome expected) {
		assertThat(gateway.check(dni).outcome()).isEqualTo(expected);
		assertThat(gateway.check(dni).outcome()).isEqualTo(expected);
	}
}
