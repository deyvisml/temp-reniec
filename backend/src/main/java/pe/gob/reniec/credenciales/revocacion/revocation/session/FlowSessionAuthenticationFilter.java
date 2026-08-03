package pe.gob.reniec.credenciales.revocacion.revocation.session;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FlowSessionAuthenticationFilter extends OncePerRequestFilter {
	private final FlowSessionCookieService cookies;
	private final FlowSessionService sessions;
	public FlowSessionAuthenticationFilter(FlowSessionCookieService cookies, FlowSessionService sessions) {
		this.cookies = cookies; this.sessions = sessions;
	}
	@Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain) throws ServletException, IOException {
		cookies.access(request).ifPresent(raw -> {
			try {
				FlowSessionService.CurrentSession current = sessions.current(raw);
				SecurityContextHolder.getContext().setAuthentication(
						new UsernamePasswordAuthenticationToken(current.sessionId(), null, java.util.List.of()));
			} catch (FlowSessionException ignored) { SecurityContextHolder.clearContext(); }
		});
		chain.doFilter(request, response);
	}
}
