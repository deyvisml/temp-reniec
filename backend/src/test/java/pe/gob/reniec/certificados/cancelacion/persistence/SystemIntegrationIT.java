package pe.gob.reniec.certificados.cancelacion.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import pe.gob.reniec.certificados.cancelacion.shared.web.CorrelationIdFilter;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "debug=false")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SystemIntegrationIT extends MySqlContainerSupport {

	private static final String LOCAL_ORIGIN = "http://localhost:3000";
	private final HttpClient httpClient = HttpClient.newHttpClient();

	@LocalServerPort
	int port;

	@Test
	void statusQueriesMySqlAndPropagatesCorrelation() throws Exception {
		HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/v1/system/status"))
				.header(CorrelationIdFilter.HEADER_NAME, "integration-status-123")
				.GET().build());

		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.headers().firstValue(CorrelationIdFilter.HEADER_NAME))
				.contains("integration-status-123");
		assertThat(response.body()).contains("\"status\":\"UP\"", "\"database\":\"UP\"", "\"timestamp\":")
				.doesNotContain("jdbc", "mysql", "SELECT", "password");
	}

	@Test
	void corsAllowsOnlyConfiguredLocalOriginAndExposesCorrelation() throws Exception {
		HttpRequest allowed = HttpRequest.newBuilder(uri("/api/v1/system/status"))
				.header(HttpHeaders.ORIGIN, LOCAL_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, CorrelationIdFilter.HEADER_NAME)
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
		HttpResponse<String> allowedResponse = send(allowed);

		assertThat(allowedResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(allowedResponse.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
				.contains(LOCAL_ORIGIN);
		assertThat(allowedResponse.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
				.contains("true");
		assertThat(allowedResponse.headers().firstValue(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
				.hasValueSatisfying(value -> assertThat(value).containsIgnoringCase(CorrelationIdFilter.HEADER_NAME));

		HttpResponse<String> rejected = send(HttpRequest.newBuilder(uri("/api/v1/system/status"))
				.header(HttpHeaders.ORIGIN, "https://untrusted.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build());
		assertThat(rejected.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty();
	}

	@Test
	void openApiContainsOnlyTheVersionedApplicationContract() throws Exception {
		HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/v3/api-docs")).GET().build());

		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.body())
				.contains("/api/v1/system/status", "SystemStatusResponse", "ApiError", "503", "X-Correlation-ID")
				.doesNotContain("/actuator", "/__test/");
	}

	private URI uri(String path) {
		return URI.create("http://localhost:" + port + path);
	}

	private HttpResponse<String> send(HttpRequest request) throws Exception {
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}
}
