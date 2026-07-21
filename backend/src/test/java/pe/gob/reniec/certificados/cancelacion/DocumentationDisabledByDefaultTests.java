package pe.gob.reniec.certificados.cancelacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import pe.gob.reniec.certificados.cancelacion.cancellation.eligibility.CertificateAvailabilityPort;
import pe.gob.reniec.certificados.cancelacion.cancellation.eligibility.EligibilityPersistenceCoordinator;
import pe.gob.reniec.certificados.cancelacion.system.SystemStatusService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"debug=false",
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
				+ "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
class DocumentationDisabledByDefaultTests {

	@MockitoBean
	SystemStatusService systemStatusService;

	@MockitoBean
	EligibilityPersistenceCoordinator eligibilityPersistenceCoordinator;

	@MockitoBean
	CertificateAvailabilityPort certificateEligibilityGateway;

	@LocalServerPort
	private int port;

	@Test
	void openApiAndSwaggerUiAreNotExposedWithoutADevelopmentProfile() throws Exception {
		HttpClient client = HttpClient.newHttpClient();

		HttpResponse<String> openApi = client.send(
				HttpRequest.newBuilder(uri("/v3/api-docs")).GET().build(), HttpResponse.BodyHandlers.ofString());
		HttpResponse<String> swagger = client.send(
				HttpRequest.newBuilder(uri("/swagger-ui.html")).GET().build(), HttpResponse.BodyHandlers.ofString());

		assertThat(openApi.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(swagger.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
	}

	private URI uri(String path) {
		return URI.create("http://localhost:" + port + path);
	}
}
