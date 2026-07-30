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
	void corsPreflightReachesProtectedIdentityEndpointBeforeAuthentication() throws Exception {
		HttpRequest preflight = HttpRequest.newBuilder(uri("/api/v1/identity-verifications"))
				.header(HttpHeaders.ORIGIN, LOCAL_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
						HttpHeaders.CONTENT_TYPE + ", " + CorrelationIdFilter.HEADER_NAME)
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();

		HttpResponse<String> response = send(preflight);

		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
				.contains(LOCAL_ORIGIN);
		assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
				.hasValueSatisfying(value -> assertThat(value).containsIgnoringCase("POST"));
		assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
				.contains("true");
	}

	@Test
	void certificateOperationsRequireTheAuthenticatedFlowAndAllowTheLocalOrigin() throws Exception {
		HttpResponse<String> listing = send(HttpRequest.newBuilder(
				uri("/api/v1/cancellation-requests/current/certificates")).GET().build());
		HttpResponse<String> listingPreflight = send(HttpRequest.newBuilder(
				uri("/api/v1/cancellation-requests/current/certificates"))
				.header(HttpHeaders.ORIGIN, LOCAL_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Correlation-ID")
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build());
		HttpResponse<String> selection = send(HttpRequest.newBuilder(
				uri("/api/v1/cancellation-requests/current/certificate-selection"))
				.header(HttpHeaders.ORIGIN, LOCAL_ORIGIN)
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.PUT(HttpRequest.BodyPublishers.ofString(
						"{\"certificateUuid\":\"11111111-1111-4111-8111-111111111111\"}"))
				.build());
		HttpResponse<String> preflight = send(HttpRequest.newBuilder(
				uri("/api/v1/cancellation-requests/current/certificate-selection"))
				.header(HttpHeaders.ORIGIN, LOCAL_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.CONTENT_TYPE)
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build());

		assertThat(listing.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertThat(listingPreflight.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(listingPreflight.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
				.contains(LOCAL_ORIGIN);
		assertThat(listingPreflight.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
				.hasValueSatisfying(value -> assertThat(value).containsIgnoringCase("X-Correlation-ID"));
		assertThat(selection.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
		assertThat(listing.body() + selection.body()).contains("SESSION_REQUIRED")
				.doesNotContain("11111111-1111-4111-8111-111111111111");
		assertThat(preflight.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(preflight.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
				.contains(LOCAL_ORIGIN);
		assertThat(preflight.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
				.hasValueSatisfying(value -> assertThat(value).containsIgnoringCase("PUT"));
	}

	@Test
	void openApiContainsApplicationAndExposedOperationalContract() throws Exception {
		HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/v3/api-docs")).GET().build());

		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.body())
				.contains("/api/v1/system/status", "/api/v1/cancellation-requests", "/actuator/health",
						"SystemStatusResponse", "StartCancellationRequest", "ApiError", "availabilityResult",
						"AVAILABLE", "NOT_AVAILABLE", "503", "X-Correlation-ID",
						"/api/v1/identity-verifications", "FlowSessionCookie", "FlowRefreshCookie", "securitySchemes",
						"/api/v1/cancellation-requests/current/certificates",
						"/api/v1/cancellation-requests/current/certificate-selection",
						"certificateUuid", "orderNumber", "emissionCreatedAt")
				.doesNotContain("/actuator/info", "/__test/", "bearerFormat", "eligibilityResult",
						"certificateCount");
	}

	private URI uri(String path) {
		return URI.create("http://localhost:" + port + path);
	}

	private HttpResponse<String> send(HttpRequest request) throws Exception {
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}
}
