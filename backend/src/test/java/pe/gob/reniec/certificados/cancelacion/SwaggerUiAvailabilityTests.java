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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import pe.gob.reniec.certificados.cancelacion.cancellation.eligibility.EligibilityPersistenceCoordinator;
import pe.gob.reniec.certificados.cancelacion.system.SystemStatusService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"debug=false",
		"springdoc.swagger-ui.enabled=true",
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
				+ "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
@ActiveProfiles("test")
class SwaggerUiAvailabilityTests {

	@MockitoBean
	SystemStatusService systemStatusService;

	@MockitoBean
	EligibilityPersistenceCoordinator eligibilityPersistenceCoordinator;

	@LocalServerPort
	private int port;

	@Test
	void swaggerUiRedirectsToThePackagedInterface() throws Exception {
		HttpClient client = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NEVER)
				.build();
		HttpResponse<String> response = client.send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/swagger-ui.html")).GET().build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isIn(HttpStatus.OK.value(), HttpStatus.FOUND.value());
		if (response.statusCode() == HttpStatus.FOUND.value()) {
			assertThat(response.headers().firstValue("location")).hasValueSatisfying(
					location -> assertThat(location).contains("/swagger-ui/index.html"));
		}
	}
}
