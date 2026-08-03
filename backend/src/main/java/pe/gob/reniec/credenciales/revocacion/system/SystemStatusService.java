package pe.gob.reniec.credenciales.revocacion.system;

import java.time.Instant;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public final class SystemStatusService {

	private final JdbcTemplate jdbcTemplate;

	public SystemStatusService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public SystemStatusResponse getStatus() {
		try {
			Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
			if (!Integer.valueOf(1).equals(result)) {
				throw new DependencyUnavailableException(null);
			}
			return new SystemStatusResponse("UP", "UP", Instant.now());
		}
		catch (DataAccessException exception) {
			throw new DependencyUnavailableException(exception);
		}
	}
}
