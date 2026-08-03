package pe.gob.reniec.credenciales.revocacion.revocation.session;

import java.time.*;
import java.util.Optional;
import jakarta.servlet.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class FlowSessionCookieService {
	private final FlowSessionProperties properties;
	public FlowSessionCookieService(FlowSessionProperties properties) { this.properties = properties; }
	public Optional<String> access(HttpServletRequest request) { return read(request, properties.getAccessCookieName()); }
	public Optional<String> refresh(HttpServletRequest request) { return read(request, properties.getRefreshCookieName()); }
	private Optional<String> read(HttpServletRequest request, String name) {
		if (request.getCookies() == null) return Optional.empty();
		return java.util.Arrays.stream(request.getCookies()).filter(c -> name.equals(c.getName()) && !c.getValue().isBlank())
				.map(Cookie::getValue).findFirst();
	}
	public ResponseCookie access(String value, Instant expiry) { return cookie(properties.getAccessCookieName(), value, expiry, "/"); }
	public ResponseCookie refresh(String value, Instant expiry) { return cookie(properties.getRefreshCookieName(), value, expiry, "/"); }
	public ResponseCookie clearAccess() { return clear(properties.getAccessCookieName(), "/"); }
	public ResponseCookie clearRefresh() { return clear(properties.getRefreshCookieName(), "/"); }
	public HttpHeaders headers(FlowSessionService.Tokens tokens) {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, access(tokens.access().value(), tokens.access().expiresAt()).toString());
		headers.add(HttpHeaders.SET_COOKIE, refresh(tokens.refresh().value(), tokens.refresh().expiresAt()).toString());
		return headers;
	}
	public HttpHeaders clearHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, clearAccess().toString());
		headers.add(HttpHeaders.SET_COOKIE, clearRefresh().toString());
		return headers;
	}
	private ResponseCookie cookie(String name, String value, Instant expiry, String path) {
		Duration age = Duration.between(Instant.now(), expiry);
		return base(name, value, path).maxAge(age.isNegative() ? Duration.ZERO : age).build();
	}
	private ResponseCookie clear(String name, String path) { return base(name, "", path).maxAge(Duration.ZERO).build(); }
	private ResponseCookie.ResponseCookieBuilder base(String name, String value, String path) {
		return ResponseCookie.from(name, value).httpOnly(true).secure(properties.isCookieSecure()).sameSite("Lax").path(path);
	}
}
