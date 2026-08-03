package pe.gob.reniec.credenciales.revocacion.shared.web;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-Correlation-ID";
	public static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";
	public static final String MDC_KEY = "correlationId";

	private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdFilter.class);
	private static final Pattern VALID_CORRELATION_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));

		request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
		response.setHeader(HEADER_NAME, correlationId);
		MDC.put(MDC_KEY, correlationId);

		try {
			filterChain.doFilter(request, response);
		}
		finally {
			LOGGER.info("HTTP request completed method={} path={} status={}", request.getMethod(),
					request.getRequestURI(), response.getStatus());
			MDC.remove(MDC_KEY);
		}
	}

	private String resolveCorrelationId(String candidate) {
		if (candidate != null && VALID_CORRELATION_ID.matcher(candidate).matches()) {
			return candidate;
		}
		return UUID.randomUUID().toString();
	}
}
