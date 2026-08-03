package pe.gob.reniec.credenciales.revocacion.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

class SystemStatusServiceTests {

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final SystemStatusService service = new SystemStatusService(jdbcTemplate);

	@Test
	void reportsOnlyControlledAvailabilityFields() {
		when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

		SystemStatusResponse response = service.getStatus();

		assertThat(response.status()).isEqualTo("UP");
		assertThat(response.database()).isEqualTo("UP");
		assertThat(response.timestamp()).isNotNull();
	}

	@Test
	void convertsDatabaseDetailsIntoTechnicalException() {
		when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
				.thenThrow(new DataAccessResourceFailureException("jdbc:mysql://secret-host SQL SELECT 1"));

		assertThatThrownBy(service::getStatus).isInstanceOf(DependencyUnavailableException.class);
	}
}
