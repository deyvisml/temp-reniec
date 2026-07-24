package pe.gob.reniec.certificados.cancelacion.cancellation.certificates;

import java.time.Duration;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("app.certificate-listing")
public class CertificateListingProperties {

	private Mode mode = Mode.DISABLED;
	private Duration staleReservationThreshold = Duration.ofSeconds(30);

	@PostConstruct
	void validate() {
		if (staleReservationThreshold == null || staleReservationThreshold.isZero()
				|| staleReservationThreshold.isNegative()) {
			throw new IllegalStateException("app.certificate-listing.stale-reservation-threshold must be positive");
		}
		if (mode == Mode.REAL) {
			throw new IllegalStateException("Real certificate listing requires the verified institutional contract");
		}
	}

	public Mode getMode() { return mode; }
	public void setMode(Mode mode) { this.mode = mode; }
	public Duration getStaleReservationThreshold() { return staleReservationThreshold; }
	public void setStaleReservationThreshold(Duration value) { staleReservationThreshold = value; }

	public enum Mode { MOCK, REAL, DISABLED }
}
