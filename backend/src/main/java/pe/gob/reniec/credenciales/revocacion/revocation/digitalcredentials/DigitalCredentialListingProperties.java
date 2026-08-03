package pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials;

import java.time.Duration;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("app.digital-credential-listing")
public class DigitalCredentialListingProperties {

	private Duration staleReservationThreshold = Duration.ofSeconds(30);

	@PostConstruct
	void validate() {
		if (staleReservationThreshold == null || staleReservationThreshold.isZero()
				|| staleReservationThreshold.isNegative()) {
			throw new IllegalStateException("app.digital-credential-listing.stale-reservation-threshold must be positive");
		}
	}

	public Duration getStaleReservationThreshold() { return staleReservationThreshold; }
	public void setStaleReservationThreshold(Duration value) { staleReservationThreshold = value; }
}
