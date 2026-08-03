package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import java.time.Duration;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationResult;

@Component
@ConfigurationProperties("app.revocation")
public class RevocationProperties {
	private RevocationResult mockOutcome = RevocationResult.SUCCEEDED;
	private Duration staleSubmissionThreshold = Duration.ofSeconds(30);
	private Duration propagationDelay = Duration.ofMinutes(1);

	@PostConstruct
	void validate() {
		if (mockOutcome == null) {
			throw new IllegalStateException("Revocation mock outcome is required");
		}
		if (staleSubmissionThreshold == null || staleSubmissionThreshold.isZero()
				|| staleSubmissionThreshold.isNegative()) {
			throw new IllegalStateException("app.revocation.stale-submission-threshold must be positive");
		}
		if (propagationDelay == null || propagationDelay.isNegative()) {
			throw new IllegalStateException("app.revocation.propagation-delay must not be negative");
		}
	}

	public RevocationResult getMockOutcome() { return mockOutcome; }
	public void setMockOutcome(RevocationResult mockOutcome) { this.mockOutcome = mockOutcome; }
	public Duration getStaleSubmissionThreshold() { return staleSubmissionThreshold; }
	public void setStaleSubmissionThreshold(Duration value) { staleSubmissionThreshold = value; }
	public Duration getPropagationDelay() { return propagationDelay; }
	public void setPropagationDelay(Duration value) { propagationDelay = value; }
}
