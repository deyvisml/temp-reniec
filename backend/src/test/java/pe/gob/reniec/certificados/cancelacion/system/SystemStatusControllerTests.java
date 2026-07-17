package pe.gob.reniec.certificados.cancelacion.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SystemStatusControllerTests {

	@Test
	void delegatesToTheTechnicalService() {
		SystemStatusService service = mock(SystemStatusService.class);
		SystemStatusResponse expected = new SystemStatusResponse("UP", "UP", Instant.parse("2026-07-16T12:00:00Z"));
		when(service.getStatus()).thenReturn(expected);

		assertThat(new SystemStatusController(service).getStatus()).isSameAs(expected);
	}
}
