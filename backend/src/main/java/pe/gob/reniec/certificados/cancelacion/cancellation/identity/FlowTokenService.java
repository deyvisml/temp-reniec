package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

@Component
public class FlowTokenService {

	private static final String AUDIENCE = "cancelacion-certificados-flow";
	private static final String PURPOSE = "purpose";
	private static final String REQUEST_ID = "request_id";
	private static final String ATTEMPT_ID = "attempt_id";
	private final IdPeruProperties properties;
	private final byte[] secret;

	public FlowTokenService(IdPeruProperties properties, IdPeruFlowKeys keys) {
		this.properties = properties;
		this.secret = keys.flowSigningKey();
	}

	public IssuedFlowToken issueIdentityInit(Long requestId) {
		return issue(FlowTokenPurpose.IDENTITY_INIT, requestId, null,
				properties.getIdentityInitTtl());
	}

	public IssuedFlowToken issueFlowAuthorization(Long requestId, Long attemptId) {
		return issue(FlowTokenPurpose.FLOW_AUTH, requestId, attemptId,
				properties.getFlowAuthorizationTtl());
	}

	private IssuedFlowToken issue(FlowTokenPurpose purpose, Long requestId, Long attemptId,
			java.time.Duration ttl) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(ttl);
		String jti = UUID.randomUUID().toString();
		JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
				.audience(AUDIENCE)
				.issueTime(Date.from(issuedAt))
				.expirationTime(Date.from(expiresAt))
				.jwtID(jti)
				.claim(PURPOSE, purpose.name())
				.claim(REQUEST_ID, requestId);
		if (attemptId != null) claims.claim(ATTEMPT_ID, attemptId);
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
		try {
			jwt.sign(new MACSigner(secret));
			return new IssuedFlowToken(jwt.serialize(), jti, expiresAt);
		}
		catch (JOSEException exception) {
			throw new IdentityIntegrationException(IdentityFailure.CONFIGURATION, "No se pudo emitir continuidad", exception);
		}
	}

	public FlowTokenClaims validate(String token, FlowTokenPurpose... allowedPurposes) {
		try {
			SignedJWT jwt = SignedJWT.parse(token);
			if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secret))) {
				throw unauthorized();
			}
			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			if (!claims.getAudience().contains(AUDIENCE)
					|| claims.getExpirationTime() == null
					|| !claims.getExpirationTime().toInstant().isAfter(Instant.now())
					|| claims.getJWTID() == null) throw unauthorized();
			FlowTokenPurpose purpose = FlowTokenPurpose.valueOf(claims.getStringClaim(PURPOSE));
			if (java.util.Arrays.stream(allowedPurposes).noneMatch(purpose::equals)) throw unauthorized();
			Long requestId = claims.getLongClaim(REQUEST_ID);
			Long attemptId = claims.getClaim(ATTEMPT_ID) == null ? null : claims.getLongClaim(ATTEMPT_ID);
			return new FlowTokenClaims(purpose, requestId, attemptId, claims.getJWTID(),
					claims.getExpirationTime().toInstant());
		}
		catch (ParseException | JOSEException | IllegalArgumentException exception) {
			throw unauthorized();
		}
	}

	private static IdentityIntegrationException unauthorized() {
		return new IdentityIntegrationException(IdentityFailure.UNAUTHORIZED, "Continuidad inválida");
	}

	public record IssuedFlowToken(String value, String jti, Instant expiresAt) { }
	public record FlowTokenClaims(FlowTokenPurpose purpose, Long requestId, Long attemptId,
			String jti, Instant expiresAt) { }
}
