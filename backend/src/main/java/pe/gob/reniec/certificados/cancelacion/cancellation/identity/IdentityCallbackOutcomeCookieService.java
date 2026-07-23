package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import java.time.Duration;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class IdentityCallbackOutcomeCookieService {

	static final String COOKIE_NAME = "idperu_callback_outcome";
	private static final Duration MAX_AGE = Duration.ofMinutes(2);
	private final IdPeruProperties properties;

	public IdentityCallbackOutcomeCookieService(IdPeruProperties properties) {
		this.properties = properties;
	}

	public Optional<IdentityCallbackOutcome> read(HttpServletRequest request) {
		if (request.getCookies() == null) return Optional.empty();
		for (Cookie cookie : request.getCookies()) {
			if (COOKIE_NAME.equals(cookie.getName())) return IdentityCallbackOutcome.parse(cookie.getValue());
		}
		return Optional.empty();
	}

	public ResponseCookie create(IdentityCallbackOutcome outcome) {
		return base(outcome.name()).maxAge(MAX_AGE).build();
	}

	public ResponseCookie clear() {
		return base("").maxAge(Duration.ZERO).build();
	}

	private ResponseCookie.ResponseCookieBuilder base(String value) {
		return ResponseCookie.from(COOKIE_NAME, value)
				.httpOnly(true)
				.secure(properties.isCookieSecure())
				.sameSite("Lax")
				.path("/");
	}
}
