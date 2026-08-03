package pe.gob.reniec.credenciales.revocacion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import pe.gob.reniec.credenciales.revocacion.system.SystemStatusService;
import pe.gob.reniec.credenciales.revocacion.revocation.initiation.AvailabilityPersistenceCoordinator;
import pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialListingPersistenceCoordinator;

@SpringBootTest(properties = {
		"debug=false",
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
				+ "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
@ActiveProfiles("test")
class RevocacionCredencialesDigitalesBackendApplicationTests {

	@MockitoBean
	SystemStatusService systemStatusService;

	@MockitoBean
	AvailabilityPersistenceCoordinator availabilityPersistenceCoordinator;

	@MockitoBean
	DigitalCredentialListingPersistenceCoordinator digitalCredentialListingPersistenceCoordinator;

	@Test
	void contextLoads() {
	}

}
