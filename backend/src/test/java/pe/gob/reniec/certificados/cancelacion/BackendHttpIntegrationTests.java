package pe.gob.reniec.certificados.cancelacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import pe.gob.reniec.certificados.cancelacion.shared.web.CorrelationIdFilter;
import pe.gob.reniec.certificados.cancelacion.cancellation.initiation.AvailabilityPersistenceCoordinator;
import pe.gob.reniec.certificados.cancelacion.system.SystemStatusService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"debug=false",
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
				+ "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
@ActiveProfiles("test")
class BackendHttpIntegrationTests {

	@MockitoBean
	SystemStatusService systemStatusService;

	@MockitoBean
	AvailabilityPersistenceCoordinator availabilityPersistenceCoordinator;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	@Autowired
	private RequestMappingHandlerMapping requestMappingHandlerMapping;

	@Test
	void healthRespondsUpAndGeneratesCorrelation() throws Exception {
		HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/actuator/health")).GET().build());

		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.body()).contains("\"status\":\"UP\"");
		assertThat(response.headers().firstValue(CorrelationIdFilter.HEADER_NAME)).isPresent()
				.hasValueSatisfying(value -> assertThat(value).isNotBlank());
	}

	@Test
	void nonHealthActuatorEndpointIsNotExposed() throws Exception {
		HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/actuator/info")).GET().build());

		assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
	}

	@Test
	void validationErrorUsesCommonSafeFormatAndCorrelation() throws Exception {
		String correlationId = "client-validation-123";
		HttpRequest request = HttpRequest.newBuilder(uri("/__test/validation"))
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.header(CorrelationIdFilter.HEADER_NAME, correlationId)
				.POST(HttpRequest.BodyPublishers.ofString("{\"value\":\"\"}"))
				.build();

		HttpResponse<String> response = send(request);

		assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(response.headers().firstValue(CorrelationIdFilter.HEADER_NAME)).contains(correlationId);
		assertThat(response.body())
				.contains("\"code\":\"VALIDATION_ERROR\"")
				.contains("\"message\":")
				.contains("\"timestamp\":")
				.contains("\"path\":\"/__test/validation\"")
				.contains("\"correlationId\":\"" + correlationId + "\"")
				.doesNotContain("trace", "exception", "MethodArgumentNotValidException");
	}

	@Test
	void missingCaptchaUsesDedicatedSafeErrorBeforePersistence() throws Exception {
		HttpResponse<String> response = postCancellation("{\"dni\":\"00000001\"}", "captcha-required-test");

		assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(response.headers().firstValue(CorrelationIdFilter.HEADER_NAME)).contains("captcha-required-test");
		assertThat(response.body()).contains("\"code\":\"RECAPTCHA_REQUIRED\"")
				.doesNotContain("00000001", "recaptchaToken", "trace", "exception");
		verifyNoInteractions(availabilityPersistenceCoordinator);
	}

	@Test
	void rejectedCaptchaPreservesCorrelationAndDoesNotPrepareRequest() throws Exception {
		HttpResponse<String> response = postCancellation(
				"{\"dni\":\"00000001\",\"recaptchaToken\":\"test-recaptcha-invalid\"}",
				"captcha-rejected-test");

		assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(response.body()).contains("\"code\":\"RECAPTCHA_REJECTED\"", "captcha-rejected-test")
				.doesNotContain("test-recaptcha-invalid", "00000001", "trace", "exception");
		verifyNoInteractions(availabilityPersistenceCoordinator);
	}

	@Test
	void unexpectedErrorDoesNotExposeInternalDetails() throws Exception {
		HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/__test/failure")).GET().build());

		assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
		assertThat(response.body())
				.contains("\"code\":\"INTERNAL_ERROR\"")
				.doesNotContain("sensitive-internal-message", "IllegalStateException", "trace", "exception");
	}

	@Test
	void validCorrelationIsPropagatedExactly() throws Exception {
		String correlationId = "web-client_123.abc";
		HttpRequest request = HttpRequest.newBuilder(uri("/actuator/health"))
				.header(CorrelationIdFilter.HEADER_NAME, correlationId)
				.GET()
				.build();

		HttpResponse<String> response = send(request);

		assertThat(response.headers().firstValue(CorrelationIdFilter.HEADER_NAME)).contains(correlationId);
	}

	@Test
	void invalidCorrelationIsReplacedWithUuid() throws Exception {
		String invalidCorrelationId = "a".repeat(65);
		HttpRequest request = HttpRequest.newBuilder(uri("/actuator/health"))
				.header(CorrelationIdFilter.HEADER_NAME, invalidCorrelationId)
				.GET()
				.build();

		HttpResponse<String> response = send(request);
		String generatedCorrelationId = response.headers().firstValue(CorrelationIdFilter.HEADER_NAME).orElseThrow();

		assertThat(generatedCorrelationId).isNotEqualTo(invalidCorrelationId);
		assertThat(UUID.fromString(generatedCorrelationId)).isNotNull();
	}

	@Test
	@DisplayName("OpenAPI documenta todas las rutas públicas y ningún contrato inexistente")
	void openApiDocumentsEveryPublicOperationAndSchema() throws Exception {
		HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/v3/api-docs")).GET().build());

		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
		String document = response.body();
		Set<String> applicationPaths = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
				.flatMap(mapping -> mapping.getPatternValues().stream())
				.filter(path -> path.startsWith("/api/v1/"))
				.collect(Collectors.toSet());

		assertThat(applicationPaths)
				.containsExactlyInAnyOrder("/api/v1/system/status", "/api/v1/cancellation-requests");
		applicationPaths.forEach(path -> assertThat(document).contains("\"" + path + "\""));

		assertThat(document)
				.contains("\"/actuator/health\"", "getSystemStatus", "initiateCancellationRequest",
						"getActuatorHealth", "Solicitudes de cancelación", "Estado técnico",
						"StartCancellationRequest", "CancellationRequestResponse", "SystemStatusResponse",
						"ActuatorHealthResponse", "ApiError", "X-Correlation-ID",
						"VALIDATION_ERROR", "RECAPTCHA_REQUIRED", "RECAPTCHA_REJECTED",
						"RECAPTCHA_EXPIRED_OR_DUPLICATE", "RECAPTCHA_UNAVAILABLE", "RECAPTCHA_TIMEOUT",
						"RECAPTCHA_INVALID_RESPONSE", "CANCELLATION_REQUEST_IN_PROGRESS", "date-time", "[0-9]{8}",
						"\"200\"", "\"400\"", "\"409\"", "\"415\"", "\"500\"",
						"availabilityResult", "AVAILABLE", "NOT_AVAILABLE", "INCONCLUSIVE", "UNAVAILABLE", "ERROR",
						"\"502\"", "\"503\"", "\"504\"")
				.doesNotContain("/__test/", "/actuator/info", "/actuator/env", "securitySchemes",
						"reused", "publicReference", "recupera una solicitud", "inicio o recuperación",
						"eligibilityResult", "certificateUuid", "orderNumber", "emissionCreatedAt", "certificateCount",
						"00000001", "test-recaptcha-valid", "RECAPTCHA_SECRET_KEY", "jdbc:mysql", "DB_PASSWORD", "MYSQL_ROOT_PASSWORD");
	}

	@Test
	void openApiYamlIsAvailableForDevelopmentTooling() throws Exception {
		HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/v3/api-docs.yaml")).GET().build());

		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.body()).contains("openapi:", "/api/v1/cancellation-requests:", "/actuator/health:");
	}

	private URI uri(String path) {
		return URI.create("http://localhost:" + port + path);
	}

	private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> postCancellation(String body, String correlationId) throws Exception {
		return send(HttpRequest.newBuilder(uri("/api/v1/cancellation-requests"))
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.header(CorrelationIdFilter.HEADER_NAME, correlationId)
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build());
	}
}
