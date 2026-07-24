package pe.gob.reniec.certificados.cancelacion.cancellation.session;

import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class FlowSessionJwtService {
	private final FlowSessionProperties properties;
	private final byte[] key;
	public FlowSessionJwtService(FlowSessionProperties properties) { this.properties = properties; key = properties.signingKey(); }

	public IssuedToken issueAccess(Long sessionId, Long requestId) {
		return issue("access", sessionId, requestId, null, null, properties.getAccessTtl());
	}
	public IssuedToken issueRefresh(Long sessionId, Long requestId, String family, int version) {
		return issue("refresh", sessionId, requestId, family, version, properties.getRefreshTtl());
	}
	private IssuedToken issue(String type, Long sid, Long rid, String family, Integer version, java.time.Duration ttl) {
		Instant now = Instant.now(), expiry = now.plus(ttl);
		JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().issuer(properties.getIssuer()).audience(properties.getAudience())
				.issueTime(Date.from(now)).expirationTime(Date.from(expiry)).jwtID(UUID.randomUUID().toString())
				.claim("typ", type).claim("sid", sid).claim("rid", rid);
		if (family != null) builder.claim("fam", family).claim("ver", version);
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
		try { jwt.sign(new MACSigner(key)); return new IssuedToken(jwt.serialize(), expiry); }
		catch (JOSEException ex) { throw new IllegalStateException("JWT issuance failed", ex); }
	}
	public Claims validate(String raw, String expectedType) {
		try {
			SignedJWT jwt = SignedJWT.parse(raw);
			if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(key))) throw invalid();
			JWTClaimsSet c = jwt.getJWTClaimsSet();
			if (!properties.getIssuer().equals(c.getIssuer()) || !c.getAudience().contains(properties.getAudience()) || c.getJWTID() == null
					|| c.getIssueTime() == null || c.getIssueTime().toInstant().isAfter(Instant.now().plusSeconds(30))
					|| c.getExpirationTime() == null || !c.getExpirationTime().toInstant().isAfter(Instant.now())
					|| !expectedType.equals(c.getStringClaim("typ"))) throw invalid();
			Long sid = c.getLongClaim("sid"), rid = c.getLongClaim("rid");
			String family = c.getStringClaim("fam");
			Integer version = c.getClaim("ver") == null ? null : c.getIntegerClaim("ver");
			if (sid == null || sid <= 0 || rid == null || rid <= 0) throw invalid();
			if ("refresh".equals(expectedType) && (family == null || family.isBlank() || version == null || version < 1))
				throw invalid();
			if ("access".equals(expectedType) && (family != null || version != null)) throw invalid();
			return new Claims(sid, rid, c.getJWTID(), family, version);
		} catch (ParseException | JOSEException | RuntimeException ex) { throw invalid(); }
	}
	private static FlowSessionException invalid() { return new FlowSessionException(FlowSessionException.Reason.INVALID); }
	public record IssuedToken(String value, Instant expiresAt) { }
	public record Claims(Long sessionId, Long requestId, String jti, String family, Integer version) { }
}
