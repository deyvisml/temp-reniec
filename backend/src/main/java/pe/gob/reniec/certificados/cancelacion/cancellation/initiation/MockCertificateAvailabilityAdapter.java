package pe.gob.reniec.certificados.cancelacion.cancellation.initiation;

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
			case "00000001" -> completed(AvailabilityOutcome.AVAILABLE);
			case "00000002" -> completed(AvailabilityOutcome.NOT_AVAILABLE);
			case "00000003" -> failed(AvailabilityOutcome.UNAVAILABLE, "MOCK_UNAVAILABLE");
			case "00000004" -> completed(AvailabilityOutcome.INCONCLUSIVE);
			case "00000005" -> failed(AvailabilityOutcome.ERROR, "MOCK_TECHNICAL_ERROR");
			case "00000006" -> timeout();
			default -> completed(AvailabilityOutcome.INCONCLUSIVE);
		};
	}

	private AvailabilityResult timeout() {
		try {
			Thread.sleep(simulatedTimeout);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
		return failed(AvailabilityOutcome.UNAVAILABLE, "MOCK_DELAY_COMPLETED");
	}

	private AvailabilityResult completed(AvailabilityOutcome outcome) {
		return new AvailabilityResult(outcome, "mock-availability", null);
	}

	private AvailabilityResult failed(AvailabilityOutcome outcome, String technicalCode) {
		return new AvailabilityResult(outcome, null, technicalCode);
	}
}
