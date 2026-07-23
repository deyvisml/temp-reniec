package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import java.time.Duration;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class FlowCookieService {

	private final IdPeruProperties properties;

	public FlowCookieService(IdPeruProperties properties) {
		this.properties = properties;
	}

	public String read(HttpServletRequest request) {
		if (request.getCookies() == null) throw unauthorized();
		for (Cookie cookie : request.getCookies()) {
			if (properties.getCookieName().equals(cookie.getName()) && !cookie.getValue().isBlank()) {
				return cookie.getValue();
			}
		}
		throw unauthorized();
	}

	public ResponseCookie create(String value, java.time.Instant expiresAt) {
		Duration maxAge = Duration.between(java.time.Instant.now(), expiresAt);
		return base(value).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge).build();
	}

	public ResponseCookie clear() {
		return base("").maxAge(Duration.ZERO).build();
	}

	private ResponseCookie.ResponseCookieBuilder base(String value) {
		return ResponseCookie.from(properties.getCookieName(), value)
				.httpOnly(true)
				.secure(properties.isCookieSecure())
				.sameSite("Lax")
				.path("/");
	}

	private static IdentityIntegrationException unauthorized() {
		return new IdentityIntegrationException(IdentityFailure.UNAUTHORIZED, "Continuidad requerida");
	}
}
