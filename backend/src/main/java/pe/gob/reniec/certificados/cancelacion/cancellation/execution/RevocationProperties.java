package pe.gob.reniec.certificados.cancelacion.cancellation.execution;

import java.time.Duration;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationResult;

@Component
@ConfigurationProperties("app.revocation")
public class RevocationProperties {
	private Mode mode = Mode.DISABLED;
	private RevocationResult mockOutcome = RevocationResult.SUCCEEDED;
	private Duration staleSubmissionThreshold = Duration.ofSeconds(30);

	@PostConstruct
	void validate() {
		if (mode == null || mockOutcome == null) {
			throw new IllegalStateException("Revocation mode and mock outcome are required");
		}
		if (staleSubmissionThreshold == null || staleSubmissionThreshold.isZero()
				|| staleSubmissionThreshold.isNegative()) {
			throw new IllegalStateException("app.revocation.stale-submission-threshold must be positive");
		}
	}

	public Mode getMode() { return mode; }
	public void setMode(Mode mode) { this.mode = mode; }
	public RevocationResult getMockOutcome() { return mockOutcome; }
	public void setMockOutcome(RevocationResult mockOutcome) { this.mockOutcome = mockOutcome; }
	public Duration getStaleSubmissionThreshold() { return staleSubmissionThreshold; }
	public void setStaleSubmissionThreshold(Duration value) { staleSubmissionThreshold = value; }

	public enum Mode { MOCK, DISABLED }
}
