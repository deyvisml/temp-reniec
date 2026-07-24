package pe.gob.reniec.certificados.cancelacion.cancellation.session;

import java.io.IOException;
import java.time.Instant;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pe.gob.reniec.certificados.cancelacion.shared.web.CorrelationIdFilter;

@Configuration
public class FlowSecurityConfiguration {
	@Bean SecurityFilterChain flowSecurity(HttpSecurity http, FlowSessionAuthenticationFilter filter) throws Exception {
		return http.cors(Customizer.withDefaults()).csrf(csrf -> csrf.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/cancellation-requests", "/api/v1/idperu/callback",
								"/api/v1/session/refresh", "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/api/v1/identity-verifications/**", "/api/v1/session/current", "/api/v1/session/logout").authenticated()
						.requestMatchers("/api/v1/cancellation-requests/current/**").authenticated()
						.requestMatchers("/api/v1/cancellation-flow/**").authenticated()
						.anyRequest().permitAll())
				.exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, ex) -> unauthorized(request, response)))
				.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class).build();
	}
	private static void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value()); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"code\":\"SESSION_REQUIRED\",\"message\":\"Inicia nuevamente el proceso para continuar.\","
				+ "\"timestamp\":\"" + Instant.now() + "\",\"path\":\"" + request.getRequestURI()
				+ "\",\"correlationId\":\"" + request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE) + "\"}");
	}
}
