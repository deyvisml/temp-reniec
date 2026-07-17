package pe.gob.reniec.certificados.cancelacion.persistence;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.mysql.MySQLContainer;

abstract class MySqlContainerSupport {

	@Container
	@ServiceConnection
	static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.0")
			.withDatabaseName("cancelacion_certificados_test");
}
