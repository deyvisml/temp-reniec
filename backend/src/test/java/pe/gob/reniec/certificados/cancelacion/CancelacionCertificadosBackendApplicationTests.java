package pe.gob.reniec.certificados.cancelacion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import pe.gob.reniec.certificados.cancelacion.system.SystemStatusService;
import pe.gob.reniec.certificados.cancelacion.cancellation.initiation.AvailabilityPersistenceCoordinator;

@SpringBootTest(properties = {
		"debug=false",
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
				+ "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
@ActiveProfiles("test")
class CancelacionCertificadosBackendApplicationTests {

	@MockitoBean
	SystemStatusService systemStatusService;

	@MockitoBean
	AvailabilityPersistenceCoordinator availabilityPersistenceCoordinator;

	@Test
	void contextLoads() {
	}

}
