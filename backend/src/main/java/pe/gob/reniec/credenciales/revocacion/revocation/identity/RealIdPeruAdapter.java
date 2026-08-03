package pe.gob.reniec.credenciales.revocacion.revocation.identity;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.nimbusds.jwt.JWTClaimsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(RealIdPeruAdapter.class);
	private static final Pattern DNI = Pattern.compile("[0-9]{8}");
	private static final int USERINFO_MAX_ATTEMPTS = 3;
	private static final long USERINFO_RETRY_DELAY_MILLIS = 250L;
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
			TokenResponse token = exchangeCode(code, codeVerifier);
			if (token == null || blank(token.accessToken()) || token.expiresIn() <= 0) throw invalid();
			JWTClaimsSet idClaims = properties.usesPkce() ? jwtValidator.validate(token.idToken(), true) : null;
			UserInfoEnvelope response = requestUserInfo(token.accessToken());
			if (properties.usesPkce()) {
				if (response == null || blank(response.jwt())) throw invalid();
				JWTClaimsSet userClaims = jwtValidator.validate(response.jwt(), false);
				String doc = userClaims.getStringClaim("doc");
				String firstName = VerifiedFirstName.normalize(userClaims.getStringClaim("first_name"));
				if (!validCitizen(doc, userClaims.getSubject()) || !idClaims.getSubject().equals(userClaims.getSubject())) throw invalid();
				return new VerifiedCitizen(userClaims.getSubject(), doc, firstName, sessionState);
			}
			if (response == null || !validCitizen(response.doc(), response.sub())) throw invalid();
			return new VerifiedCitizen(response.sub(), response.doc(),
					VerifiedFirstName.normalize(response.first_name()), sessionState);
		}
		catch (IdentityIntegrationException exception) { throw exception; }
		catch (Exception exception) { throw new IdentityIntegrationException(IdentityFailure.INVALID_RESPONSE, "Respuesta inválida de ID Perú", exception); }
	}

	private TokenResponse exchangeCode(String code, String codeVerifier) {
		LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("code", code);
		form.add("redirect_uri", properties.getRedirectUri().toString());
		form.add("client_id", properties.getClientId());
		form.add("client_secret", properties.getClientSecret());
		if (properties.usesPkce()) form.add("code_verifier", codeVerifier);
		try {
			return client.post().uri(properties.getTokenUri())
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.accept(MediaType.APPLICATION_JSON)
					.headers(headers -> addRefererForV2(headers))
					.body(form).retrieve().body(TokenResponse.class);
		}
		catch (RestClientResponseException exception) {
			int status = exception.getStatusCode().value();
			LOGGER.warn("ID Peru token exchange failed with status {}", status);
			IdentityFailure failure = exception.getStatusCode().is4xxClientError()
					? IdentityFailure.TOKEN_REJECTED : IdentityFailure.UNAVAILABLE;
			throw providerFailure(failure, "TOKEN_HTTP_" + status, exception);
		}
		catch (RestClientException exception) {
			logTransportFailure("TOKEN", exception);
			throw transportFailure("TOKEN", exception);
		}
	}

	private UserInfoEnvelope requestUserInfo(String accessToken) {
		for (int attempt = 1; attempt <= USERINFO_MAX_ATTEMPTS; attempt++) {
			try {
				return client.post().uri(properties.getUserinfoUri())
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.accept(MediaType.APPLICATION_JSON)
						.headers(headers -> addRefererForV2(headers))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.retrieve().body(UserInfoEnvelope.class);
			}
			catch (RestClientResponseException exception) {
				if (attempt < USERINFO_MAX_ATTEMPTS && exception.getStatusCode().is5xxServerError()) {
					LOGGER.warn("ID Peru userinfo returned status {}; retrying ({}/{})",
							exception.getStatusCode().value(), attempt, USERINFO_MAX_ATTEMPTS);
					pauseBeforeUserInfoRetry();
					continue;
				}
				LOGGER.warn("ID Peru userinfo failed after {} attempt(s) with status {}",
						attempt, exception.getStatusCode().value());
				int status = exception.getStatusCode().value();
				IdentityFailure failure = exception.getStatusCode().is4xxClientError()
						? IdentityFailure.TOKEN_REJECTED : IdentityFailure.UNAVAILABLE;
				throw providerFailure(failure, "USERINFO_HTTP_" + status, exception);
			}
			catch (RestClientException exception) {
				logTransportFailure("USERINFO", exception);
				throw transportFailure("USERINFO", exception);
			}
		}
		throw new IllegalStateException("Unreachable userinfo retry state");
	}

	private static void pauseBeforeUserInfoRetry() {
		try {
			Thread.sleep(USERINFO_RETRY_DELAY_MILLIS);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IdentityIntegrationException(IdentityFailure.UNAVAILABLE,
					"USERINFO_RETRY_INTERRUPTED", "ID Perú no disponible", exception);
		}
	}

	private static IdentityIntegrationException transportFailure(String phase, RestClientException exception) {
		Throwable cause = rootCause(exception);
		while (cause != null) {
			if (cause instanceof SocketTimeoutException) {
				return new IdentityIntegrationException(IdentityFailure.TIMEOUT,
						phase + "_TIMEOUT", "Timeout de ID Perú", exception);
			}
			cause = cause.getCause();
		}
		return providerFailure(IdentityFailure.UNAVAILABLE,
				phase + "_" + diagnosticCode(rootCause(exception)), exception);
	}

	private static void logTransportFailure(String phase, RestClientException exception) {
		LOGGER.warn("ID Peru {} transport failure type={} rootCause={}", phase,
				exception.getClass().getSimpleName(), rootCause(exception).getClass().getSimpleName());
	}

	private static Throwable rootCause(Throwable exception) {
		Throwable root = exception;
		while (root.getCause() != null && root.getCause() != root) root = root.getCause();
		return root;
	}

	private static String diagnosticCode(Throwable exception) {
		String simpleName = exception.getClass().getSimpleName();
		StringBuilder code = new StringBuilder(simpleName.length() + 8);
		for (int index = 0; index < simpleName.length(); index++) {
			char current = simpleName.charAt(index);
			if (index > 0 && Character.isUpperCase(current)) code.append('_');
			code.append(Character.toUpperCase(current));
		}
		return code.toString();
	}

	private static IdentityIntegrationException providerFailure(IdentityFailure failure,
			String technicalCode, Exception cause) {
		return new IdentityIntegrationException(failure, technicalCode, "ID Perú no disponible", cause);
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
	private record UserInfoEnvelope(String jwt, String sub, String doc, String first_name) { }
}
