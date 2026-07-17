package pe.gob.reniec.certificados.cancelacion.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

	@Bean
	OpenAPI cancellationCertificatesOpenApi() {
		return new OpenAPI().info(new Info()
				.title("API de cancelación de certificados digitales")
				.description("Contrato técnico versionado del sistema")
				.version("v1"));
	}
}
