package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.id-peru", name = "mode", havingValue = "real")
public class IdPeruJwtValidator {
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
		try {
			String body = client.get().uri(properties.getJwksUri()).accept(MediaType.APPLICATION_JSON)
					.retrieve().body(String.class);
			cached = JWKSet.parse(body);
			cacheExpiresAt = Instant.now().plus(properties.getJwksTtl());
			return cached;
		}
		catch (Exception exception) {
			throw new IdentityIntegrationException(IdentityFailure.UNAVAILABLE, "No se pudieron validar las llaves públicas", exception);
		}
	}

	private static IdentityIntegrationException invalid() {
		return new IdentityIntegrationException(IdentityFailure.TOKEN_INVALID, "JWT de ID Perú inválido");
	}
}
