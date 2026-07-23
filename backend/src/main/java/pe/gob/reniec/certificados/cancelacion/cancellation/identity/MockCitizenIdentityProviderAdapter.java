package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(prefix = "app.id-peru", name = "mode", havingValue = "mock")
public class MockCitizenIdentityProviderAdapter implements CitizenIdentityProviderPort {
	private final IdPeruProperties properties;

	public MockCitizenIdentityProviderAdapter(IdPeruProperties properties) { this.properties = properties; }

	@Override
	public URI authorizationUri(AuthorizationContext context) {
		return UriComponentsBuilder.fromUriString(properties.getRedirectUri().toString())
				.replacePath("/api/v1/identity-verifications/mock/authorize")
				.queryParam("state", context.state()).build().encode().toUri();
	}

	@Override
	public VerifiedCitizen authenticate(String code, String sessionState, String codeVerifier, String expectedDni) {
		return switch (properties.getMockScenario().toUpperCase()) {
			case "MATCH" -> new VerifiedCitizen("mock-subject", expectedDni, "mock-reference");
			case "MISMATCH" -> new VerifiedCitizen("mock-subject", "99999999", "mock-reference");
			case "TIMEOUT" -> throw new IdentityIntegrationException(IdentityFailure.TIMEOUT, "Timeout simulado");
			case "UNAVAILABLE" -> throw new IdentityIntegrationException(IdentityFailure.UNAVAILABLE, "Indisponibilidad simulada");
			case "INVALID" -> throw new IdentityIntegrationException(IdentityFailure.INVALID_RESPONSE, "Respuesta inválida simulada");
			default -> throw new IdentityIntegrationException(IdentityFailure.REJECTED, "Autenticación rechazada");
		};
	}
}
