package pe.gob.reniec.credenciales.revocacion.revocation.initiation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.credential-provider", name = "mode", havingValue = "disabled", matchIfMissing = true)
final class DisabledDigitalCredentialAvailabilityAdapter implements DigitalCredentialAvailabilityPort {
	@Override
	public AvailabilityResult checkAvailability(String dni) {
		return new AvailabilityResult(AvailabilityOutcome.UNAVAILABLE, null, "CREDENTIAL_PROVIDER_DISABLED");
	}
}
