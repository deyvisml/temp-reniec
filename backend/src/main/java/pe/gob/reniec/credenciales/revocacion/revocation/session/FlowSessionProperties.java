package pe.gob.reniec.credenciales.revocacion.revocation.session;

import java.time.Duration;
import java.util.Base64;
import java.security.SecureRandom;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("app.session")
public class FlowSessionProperties {
	private String signingSecret;
	private String issuer = "revocacion-credenciales-digitales-backend";
	private String audience = "revocacion-credenciales-digitales-flow";
	private Duration accessTtl = Duration.ofMinutes(15);
	private Duration refreshTtl = Duration.ofDays(3);
	private Duration concurrentRefreshWindow = Duration.ofSeconds(5);
	private boolean cookieSecure = true;
	private String accessCookieName = "revocacion_access";
	private String refreshCookieName = "revocacion_refresh";
	private byte[] resolvedKey;

	@PostConstruct public void validate() {
		if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank())
			throw new IllegalStateException("app.session issuer and audience are required");
		if (!positive(accessTtl) || !positive(refreshTtl) || !positive(concurrentRefreshWindow))
			throw new IllegalStateException("app.session durations must be positive");
		if (accessTtl.compareTo(refreshTtl) >= 0)
			throw new IllegalStateException("app.session access-ttl must be shorter than refresh-ttl");
		if (concurrentRefreshWindow.compareTo(accessTtl) >= 0)
			throw new IllegalStateException("app.session concurrent-refresh-window must be shorter than access-ttl");
		if (signingSecret == null || signingSecret.isBlank()) {
			resolvedKey = new byte[32];
			new SecureRandom().nextBytes(resolvedKey);
			return;
		}
		try {
			byte[] key = Base64.getDecoder().decode(signingSecret);
			if (key.length < 32) throw new IllegalArgumentException();
			resolvedKey = key;
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException("app.session.signing-secret must be Base64 with at least 32 bytes");
		}
	}
	private static boolean positive(Duration value) { return value != null && !value.isZero() && !value.isNegative(); }
	public byte[] signingKey() { return resolvedKey.clone(); }
	public String getSigningSecret() { return signingSecret; }
	public void setSigningSecret(String value) { signingSecret = value; }
	public String getIssuer() { return issuer; }
	public void setIssuer(String value) { issuer = value; }
	public String getAudience() { return audience; }
	public void setAudience(String value) { audience = value; }
	public Duration getAccessTtl() { return accessTtl; }
	public void setAccessTtl(Duration value) { accessTtl = value; }
	public Duration getRefreshTtl() { return refreshTtl; }
	public void setRefreshTtl(Duration value) { refreshTtl = value; }
	public Duration getConcurrentRefreshWindow() { return concurrentRefreshWindow; }
	public void setConcurrentRefreshWindow(Duration value) { concurrentRefreshWindow = value; }
	public boolean isCookieSecure() { return cookieSecure; }
	public void setCookieSecure(boolean value) { cookieSecure = value; }
	public String getAccessCookieName() { return accessCookieName; }
	public void setAccessCookieName(String value) { accessCookieName = value; }
	public String getRefreshCookieName() { return refreshCookieName; }
	public void setRefreshCookieName(String value) { refreshCookieName = value; }
}
