package pe.gob.reniec.credenciales.revocacion.revocation.identity;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.id-peru", name = "mode", havingValue = "disabled", matchIfMissing = true)
public class DisabledCitizenIdentityProviderAdapter implements CitizenIdentityProviderPort {
	@Override public URI authorizationUri(AuthorizationContext context) { throw unavailable(); }
	@Override public VerifiedCitizen authenticate(String code, String sessionState, String verifier, String dni) { throw unavailable(); }
	private static IdentityIntegrationException unavailable() {
		return new IdentityIntegrationException(IdentityFailure.CONFIGURATION, "ID Perú no está configurado");
	}
}
