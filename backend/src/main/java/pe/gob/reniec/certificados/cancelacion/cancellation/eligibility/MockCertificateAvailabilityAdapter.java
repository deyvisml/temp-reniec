package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({ "local", "test" })
public final class MockCertificateAvailabilityAdapter implements CertificateAvailabilityPort {

	private final Duration simulatedTimeout;

	public MockCertificateAvailabilityAdapter(
			@Value("${app.availability.mock.simulated-timeout:2s}") Duration simulatedTimeout) {
		this.simulatedTimeout = simulatedTimeout;
	}

	@Override
	public AvailabilityResult checkAvailability(String dni) {
		return switch (dni) {
			case "00000001" -> result(AvailabilityOutcome.AVAILABLE, "MOCK_AVAILABLE");
			case "00000002" -> result(AvailabilityOutcome.NOT_AVAILABLE, "MOCK_NOT_AVAILABLE");
			case "00000003" -> result(AvailabilityOutcome.UNAVAILABLE, "MOCK_UNAVAILABLE");
			case "00000004" -> result(AvailabilityOutcome.INCONCLUSIVE, "MOCK_INCONCLUSIVE");
			case "00000005" -> result(AvailabilityOutcome.ERROR, "MOCK_TECHNICAL_ERROR");
			case "00000006" -> timeout();
			default -> result(AvailabilityOutcome.NOT_AVAILABLE, "MOCK_DEFAULT_NOT_AVAILABLE");
		};
	}

	private AvailabilityResult timeout() {
		try {
			Thread.sleep(simulatedTimeout);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
		return result(AvailabilityOutcome.UNAVAILABLE, "MOCK_DELAY_COMPLETED");
	}

	private AvailabilityResult result(AvailabilityOutcome outcome, String code) {
		return new AvailabilityResult(outcome, "mock-availability", code);
	}
}
