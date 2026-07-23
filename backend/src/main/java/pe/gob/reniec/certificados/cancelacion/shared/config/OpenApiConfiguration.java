package pe.gob.reniec.certificados.cancelacion.shared.config;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

import pe.gob.reniec.certificados.cancelacion.shared.web.CorrelationIdFilter;

@Configuration
public class OpenApiConfiguration {

	@Bean
	OpenAPI cancellationCertificatesOpenApi() {
		return new OpenAPI().components(new Components().addSecuritySchemes("FlowCookie",
				new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE)
						.name("cancelacion_flow").description("Continuidad temporal HttpOnly emitida por el backend; no es una sesión permanente.")))
				.info(new Info()
				.title("API de cancelación de certificados digitales")
				.description("API institucional para iniciar el flujo ciudadano. La consulta pública exige Google reCAPTCHA v2 Checkbox y la autenticación usa ID Perú con continuidad temporal HttpOnly.")
				.version("v1")
				.contact(new Contact().name("RENIEC")))
				.tags(List.of(
						new Tag().name("Solicitudes de cancelación")
								.description("Inicio de solicitudes y consulta de disponibilidad de certificados."),
						new Tag().name("Verificación de identidad")
								.description("Inicio, callback, estado e invalidación temporal de ID Perú."),
						new Tag().name("Estado técnico")
								.description("Disponibilidad operativa del backend y sus dependencias.")));
	}

	@Bean
	OpenApiCustomizer actuatorHealthDocumentation() {
		return openApi -> {
			openApi.setTags(List.of(
					new Tag().name("Solicitudes de cancelación")
							.description("Inicio de solicitudes y consulta de disponibilidad de certificados."),
					new Tag().name("Verificación de identidad")
							.description("Inicio, callback, estado e invalidación temporal de ID Perú."),
					new Tag().name("Estado técnico")
							.description("Disponibilidad operativa del backend y sus dependencias.")));
			if (openApi.getPaths() == null || openApi.getPaths().get("/actuator/health") == null
					|| openApi.getPaths().get("/actuator/health").getGet() == null) {
				return;
			}

			ObjectSchema healthSchema = new ObjectSchema();
			healthSchema.setName("ActuatorHealthResponse");
			healthSchema.setDescription("Estado agregado y seguro publicado por Spring Boot Actuator.");
			healthSchema.addProperty("status", new StringSchema().description("Estado agregado.").example("UP"));
			healthSchema.setRequired(List.of("status"));
			openApi.getComponents().addSchemas("ActuatorHealthResponse", healthSchema);

			Header correlationHeader = new Header()
					.description("Identificador de correlación de la solicitud")
					.schema(new StringSchema());
			Content healthContent = new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
					new MediaType().schema(new ObjectSchema().$ref("#/components/schemas/ActuatorHealthResponse")));

			var operation = openApi.getPaths().get("/actuator/health").getGet();
			operation.setOperationId("getActuatorHealth");
			operation.setSummary("Comprueba la salud operativa agregada");
			operation.setDescription("Informa si la aplicación y sus dependencias configuradas están operativas sin exponer detalles internos.");
			operation.setTags(List.of("Estado técnico"));
			operation.setParameters(List.of(new Parameter()
					.name(CorrelationIdFilter.HEADER_NAME)
					.in("header")
					.required(false)
					.description("Identificador opcional de correlación. Debe tener entre 1 y 64 caracteres ASCII válidos.")
					.schema(new StringSchema().maxLength(64)
							.pattern("[A-Za-z0-9][A-Za-z0-9._-]{0,63}"))));
			operation.getResponses().addApiResponse("200", new ApiResponse()
					.description("Aplicación operativa")
					.addHeaderObject(CorrelationIdFilter.HEADER_NAME, correlationHeader)
					.content(healthContent));
			operation.getResponses().addApiResponse("503", new ApiResponse()
					.description("Aplicación o dependencia no disponible")
					.addHeaderObject(CorrelationIdFilter.HEADER_NAME, correlationHeader)
					.content(healthContent));
		};
	}
}
