package pe.gob.reniec.certificados.cancelacion.cancellation.session;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.gob.reniec.certificados.cancelacion.shared.web.CorrelationIdFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class CookieMutationOriginFilter extends OncePerRequestFilter {
	private final FlowSessionCookieService cookies;
	private final List<String> allowedOrigins;
	public CookieMutationOriginFilter(FlowSessionCookieService cookies,
			@Value("${app.cors.allowed-origins:}") List<String> allowedOrigins) {
		this.cookies = cookies;
		this.allowedOrigins = allowedOrigins.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
	}
	@Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain) throws ServletException, IOException {
		String method = request.getMethod();
		boolean mutation = !("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method));
		boolean callback = "/api/v1/idperu/callback".equals(request.getRequestURI());
		String origin = request.getHeader("Origin");
		boolean authenticatedCookie = cookies.access(request).isPresent() || cookies.refresh(request).isPresent();
		if (mutation && !callback && authenticatedCookie && origin != null
				&& !allowedOrigins.contains(origin)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write(error("INVALID_ORIGIN", "Origen no permitido.", request));
			return;
		}
		chain.doFilter(request, response);
	}
	private static String error(String code, String message, HttpServletRequest request) {
		return "{\"code\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\",\"path\":\"%s\",\"correlationId\":\"%s\"}"
				.formatted(code, message, Instant.now(), request.getRequestURI(),
						String.valueOf(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE)));
	}
}
