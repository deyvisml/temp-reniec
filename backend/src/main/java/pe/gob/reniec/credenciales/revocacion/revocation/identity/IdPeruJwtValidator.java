package pe.gob.reniec.credenciales.revocacion.revocation.identity;

import java.text.ParseException;
import java.time.Instant;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(prefix = "app.id-peru", name = "mode", havingValue = "real")
public class IdPeruJwtValidator {
	private static final Logger LOGGER = LoggerFactory.getLogger(IdPeruJwtValidator.class);
	private final IdPeruProperties properties;
	private final RestClient client;
	private volatile JWKSet cached;
	private volatile Instant cacheExpiresAt = Instant.EPOCH;

	public IdPeruJwtValidator(IdPeruProperties properties, IdPeruHttpClientFactory clients) {
		this.properties = properties;
		this.client = clients.create();
	}

	public JWTClaimsSet validate(String serialized, boolean requireAudience) {
		try {
			SignedJWT jwt = SignedJWT.parse(serialized);
			if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm()) || jwt.getHeader().getKeyID() == null) throw invalid();
			RSAKey key = findKey(jwt.getHeader().getKeyID(), false);
			if (key == null) key = findKey(jwt.getHeader().getKeyID(), true);
			if (key == null || !jwt.verify(new RSASSAVerifier(key))) throw invalid();
			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			Instant now = Instant.now();
			if (claims.getExpirationTime() == null || !claims.getExpirationTime().toInstant().isAfter(now)
					|| claims.getNotBeforeTime() != null && claims.getNotBeforeTime().toInstant().isAfter(now)
					|| !properties.getIssuer().equals(claims.getIssuer())
					|| requireAudience && !claims.getAudience().contains(properties.getClientId())
					|| claims.getSubject() == null || claims.getSubject().isBlank()) throw invalid();
			return claims;
		}
		catch (ParseException | JOSEException exception) { throw invalid(); }
	}

	private RSAKey findKey(String kid, boolean forceRefresh) {
		JWKSet set = jwks(forceRefresh);
		JWK key = set.getKeyByKeyId(kid);
		return key instanceof RSAKey rsa && (rsa.getKeyUse() == null || "sig".equals(rsa.getKeyUse().identifier())) ? rsa : null;
	}

	private synchronized JWKSet jwks(boolean forceRefresh) {
		if (!forceRefresh && cached != null && cacheExpiresAt.isAfter(Instant.now())) return cached;
		long startedAt = System.nanoTime();
		try {
			LOGGER.info("ID Peru request phase=JWKS forceRefresh={}", forceRefresh);
			String body = client.get().uri(properties.getJwksUri()).accept(MediaType.APPLICATION_JSON)
					.retrieve().body(String.class);
			cached = JWKSet.parse(body);
			cacheExpiresAt = Instant.now().plus(properties.getJwksTtl());
			LOGGER.info("ID Peru response phase=JWKS outcome=SUCCESS forceRefresh={} keyCount={} durationMs={}",
					forceRefresh, cached.getKeys().size(), elapsedMillis(startedAt));
			return cached;
		}
		catch (RestClientResponseException exception) {
			int status = exception.getStatusCode().value();
			LOGGER.warn("ID Peru response phase=JWKS outcome=HTTP_ERROR httpStatus={} technicalCode={} durationMs={}",
					status, "JWKS_HTTP_" + status, elapsedMillis(startedAt));
			throw unavailable("JWKS_HTTP_" + status, exception);
		}
		catch (RestClientException exception) {
			Throwable root = rootCause(exception);
			String technicalCode = "JWKS_" + diagnosticCode(root);
			LOGGER.warn("ID Peru response phase=JWKS outcome=TRANSPORT_ERROR technicalCode={} durationMs={} exceptionType={} rootCause={}",
					technicalCode, elapsedMillis(startedAt), exception.getClass().getSimpleName(),
					root.getClass().getSimpleName());
			throw unavailable(technicalCode, exception);
		}
		catch (Exception exception) {
			LOGGER.warn("ID Peru response phase=JWKS outcome=INVALID_RESPONSE technicalCode=JWKS_INVALID_RESPONSE durationMs={} exceptionType={}",
					elapsedMillis(startedAt), exception.getClass().getSimpleName());
			throw unavailable("JWKS_INVALID_RESPONSE", exception);
		}
	}

	private static IdentityIntegrationException unavailable(String technicalCode, Exception cause) {
		return new IdentityIntegrationException(IdentityFailure.UNAVAILABLE, technicalCode,
				"No se pudieron validar las llaves públicas", cause);
	}

	private static long elapsedMillis(long startedAt) {
		return (System.nanoTime() - startedAt) / 1_000_000;
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

	private static IdentityIntegrationException invalid() {
		return new IdentityIntegrationException(IdentityFailure.TOKEN_INVALID, "JWT de ID Perú inválido");
	}
}
