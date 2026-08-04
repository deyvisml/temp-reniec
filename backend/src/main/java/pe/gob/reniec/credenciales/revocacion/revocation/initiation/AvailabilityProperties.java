package pe.gob.reniec.credenciales.revocacion.revocation.initiation;

import java.time.Duration;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("app.availability")
public class AvailabilityProperties {

	private Duration timeout = Duration.ofSeconds(15);

	@PostConstruct
	void validate() {
		if (timeout == null || timeout.isZero() || timeout.isNegative()) {
			throw new IllegalStateException("app.availability.timeout must be positive");
		}
	}

	public Duration getTimeout() { return timeout; }
	public void setTimeout(Duration timeout) { this.timeout = timeout; }
}
