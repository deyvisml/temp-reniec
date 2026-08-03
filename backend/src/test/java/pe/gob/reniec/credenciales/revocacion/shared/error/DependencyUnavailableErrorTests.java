package pe.gob.reniec.credenciales.revocacion.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import pe.gob.reniec.credenciales.revocacion.shared.web.CorrelationIdFilter;
import pe.gob.reniec.credenciales.revocacion.system.DependencyUnavailableException;

class DependencyUnavailableErrorTests {

	@Test
	void mapsDependencyFailureToSafeCommonError() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/system/status");
		request.setAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE, "test-correlation");

		var response = new GlobalExceptionHandler().handleDependencyUnavailable(
				new DependencyUnavailableException(new IllegalStateException("jdbc:mysql://secret SQL SELECT 1")),
				request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(response.getBody()).isNotNull().satisfies(error -> {
			assertThat(error.code()).isEqualTo("DEPENDENCY_UNAVAILABLE");
			assertThat(error.message()).isEqualTo("El servicio no está disponible temporalmente.");
			assertThat(error.path()).isEqualTo("/api/v1/system/status");
			assertThat(error.correlationId()).isEqualTo("test-correlation");
			assertThat(error.toString()).doesNotContain("jdbc", "SQL", "secret");
		});
	}
}
