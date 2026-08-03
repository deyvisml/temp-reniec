package pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialListingResult.Outcome;

@Configuration
class DigitalCredentialListingConfiguration {

	@Bean
	@ConditionalOnProperty(prefix = "app.credential-provider", name = "mode", havingValue = "mock")
	DigitalCredentialListingPort mockDigitalCredentialListingPort() {
		return new DeterministicDigitalCredentialListingAdapter();
	}

	@Bean
	@ConditionalOnProperty(prefix = "app.credential-provider", name = "mode", havingValue = "disabled", matchIfMissing = true)
	DigitalCredentialListingPort disabledDigitalCredentialListingPort() {
		return (dni, correlationId) -> new DigitalCredentialListingResult(Outcome.UNAVAILABLE,
				List.of(), null, "DIGITAL_CREDENTIAL_LISTING_DISABLED");
	}
}
