package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import java.net.URI;

public interface CitizenIdentityProviderPort {
	URI authorizationUri(AuthorizationContext context);
	VerifiedCitizen authenticate(String code, String sessionState, String codeVerifier, String expectedDni);

	record AuthorizationContext(String state, String codeChallenge, String dni) { }
	record VerifiedCitizen(String subject, String dni, String externalReference) { }
}
