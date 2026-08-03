package pe.gob.reniec.credenciales.revocacion.shared.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

	private final List<String> allowedOrigins;

	public WebConfiguration(@Value("${app.cors.allowed-origins:}") List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins.stream().map(String::trim).filter(value -> !value.isEmpty()).toList();
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		if (allowedOrigins.isEmpty()) {
			return;
		}

		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins.toArray(String[]::new))
				.allowedMethods("GET", "POST", "PUT", "OPTIONS")
				.allowedHeaders("Accept", "Content-Type", CorrelationIdFilter.HEADER_NAME)
				.exposedHeaders(CorrelationIdFilter.HEADER_NAME)
				.allowCredentials(true)
				.maxAge(3600);
	}
}
