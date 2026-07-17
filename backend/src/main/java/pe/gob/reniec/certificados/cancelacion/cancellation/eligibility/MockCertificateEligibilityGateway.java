package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({ "local", "test" })
public final class MockCertificateEligibilityGateway implements CertificateEligibilityGateway {

	private final Duration simulatedTimeout;

	public MockCertificateEligibilityGateway(
			@Value("${app.eligibility.mock.simulated-timeout:2s}") Duration simulatedTimeout) {
		this.simulatedTimeout = simulatedTimeout;
	}

	@Override
	public EligibilityGatewayResult check(String dni) {
		return switch (dni) {
			case "00000001" -> result(EligibilityOutcome.ELIGIBLE, "MOCK_ELIGIBLE");
			case "00000002" -> result(EligibilityOutcome.NOT_ELIGIBLE, "MOCK_NOT_ELIGIBLE");
			case "00000003" -> result(EligibilityOutcome.UNAVAILABLE, "MOCK_UNAVAILABLE");
			case "00000004" -> result(EligibilityOutcome.INCONCLUSIVE, "MOCK_INCONCLUSIVE");
			case "00000005" -> result(EligibilityOutcome.ERROR, "MOCK_TECHNICAL_ERROR");
			case "00000006" -> timeout();
			default -> result(EligibilityOutcome.NOT_ELIGIBLE, "MOCK_DEFAULT_NOT_ELIGIBLE");
		};
	}

	private EligibilityGatewayResult timeout() {
		try {
			Thread.sleep(simulatedTimeout);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
		return result(EligibilityOutcome.UNAVAILABLE, "MOCK_DELAY_COMPLETED");
	}

	private EligibilityGatewayResult result(EligibilityOutcome outcome, String code) {
		return new EligibilityGatewayResult(outcome, "mock-eligibility", code);
	}
}
