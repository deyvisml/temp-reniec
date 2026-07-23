package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(prefix = "app.id-peru", name = "mode", havingValue = "real")
public class RealIdPeruAdapter implements CitizenIdentityProviderPort {
	private static final Pattern DNI = Pattern.compile("[0-9]{8}");
	private final IdPeruProperties properties;
	private final IdPeruDniEncryptor dniEncryptor;
	private final IdPeruJwtValidator jwtValidator;
	private final RestClient client;

	public RealIdPeruAdapter(IdPeruProperties properties, IdPeruDniEncryptor dniEncryptor,
			IdPeruJwtValidator jwtValidator, IdPeruHttpClientFactory clients) {
		this.properties = properties; this.dniEncryptor = dniEncryptor;
		this.jwtValidator = jwtValidator; this.client = clients.create();
	}

	@Override
	public URI authorizationUri(AuthorizationContext context) {
		Map<String, Object> values = new LinkedHashMap<>();
		values.put("response_type", "code");
		values.put("client_id", properties.getClientId());
		values.put("redirect_uri", properties.getRedirectUri());
		values.put("state", context.state());
		values.put("scope", properties.getScope());
		if (properties.usesPkce()) {
			values.put("code_challenge", context.codeChallenge());
			values.put("code_challenge_method", "S256");
		}
		values.put("acr_values", properties.getAcrValues());
		if (!"pki_dnie".equals(properties.getAcrValues())) {
			values.put("vd", dniEncryptor.encrypt(context.dni(), properties.getClientId()));
		}

		UriComponentsBuilder builder = UriComponentsBuilder.fromUri(properties.getAuthUri());
		values.keySet().forEach(name -> builder.queryParam(name, "{" + name + "}"));
		return builder.encode().buildAndExpand(values).toUri();
	}

	@Override
	public VerifiedCitizen authenticate(String code, String sessionState, String codeVerifier, String expectedDni) {
		try {
			LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			form.add("grant_type", "authorization_code");
			form.add("code", code);
			form.add("redirect_uri", properties.getRedirectUri().toString());
			form.add("client_id", properties.getClientId());
			form.add("client_secret", properties.getClientSecret());
			if (properties.usesPkce()) form.add("code_verifier", codeVerifier);
			TokenResponse token = client.post().uri(properties.getTokenUri())
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.headers(headers -> addRefererForV2(headers))
					.body(form).retrieve().body(TokenResponse.class);
			if (token == null || blank(token.accessToken()) || token.expiresIn() <= 0) throw invalid();
			JWTClaimsSet idClaims = properties.usesPkce() ? jwtValidator.validate(token.idToken(), true) : null;
			UserInfoEnvelope response = client.post().uri(properties.getUserinfoUri())
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.headers(headers -> addRefererForV2(headers))
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken()).retrieve().body(UserInfoEnvelope.class);
			if (properties.usesPkce()) {
				if (response == null || blank(response.jwt())) throw invalid();
				JWTClaimsSet userClaims = jwtValidator.validate(response.jwt(), false);
				String doc = userClaims.getStringClaim("doc");
				if (!validCitizen(doc, userClaims.getSubject()) || !idClaims.getSubject().equals(userClaims.getSubject())) throw invalid();
				return new VerifiedCitizen(userClaims.getSubject(), doc, sessionState);
			}
			if (response == null || !validCitizen(response.doc(), response.sub())) throw invalid();
			return new VerifiedCitizen(response.sub(), response.doc(), sessionState);
		}
		catch (IdentityIntegrationException exception) { throw exception; }
		catch (RestClientResponseException exception) {
			if (exception.getStatusCode().is4xxClientError()) {
				throw new IdentityIntegrationException(IdentityFailure.TOKEN_REJECTED,
						"ID Perú rechazó la autorización", exception);
			}
			throw new IdentityIntegrationException(IdentityFailure.UNAVAILABLE,
					"ID Perú no disponible", exception);
		}
		catch (RestClientException exception) {
			Throwable cause = exception;
			while (cause != null) {
				if (cause instanceof SocketTimeoutException) {
					throw new IdentityIntegrationException(IdentityFailure.TIMEOUT,
							"Timeout de ID Perú", exception);
				}
				cause = cause.getCause();
			}
			throw new IdentityIntegrationException(IdentityFailure.UNAVAILABLE, "ID Perú no disponible", exception);
		}
		catch (Exception exception) { throw new IdentityIntegrationException(IdentityFailure.INVALID_RESPONSE, "Respuesta inválida de ID Perú", exception); }
	}

	private static boolean blank(String value) { return value == null || value.isBlank(); }
	private static boolean validCitizen(String dni, String subject) {
		return DNI.matcher(dni == null ? "" : dni).matches() && !blank(subject);
	}
	private void addRefererForV2(HttpHeaders headers) {
		if (properties.usesPkce()) headers.set(HttpHeaders.REFERER, properties.getReferer());
	}
	private static IdentityIntegrationException invalid() { return new IdentityIntegrationException(IdentityFailure.INVALID_RESPONSE, "Respuesta inválida de ID Perú"); }
	private record TokenResponse(String access_token, long expires_in, String id_token, String token_type) {
		String accessToken() { return access_token; } long expiresIn() { return expires_in; }
		String idToken() { return id_token; }
	}
	private record UserInfoEnvelope(String jwt, String sub, String doc) { }
}
