package pe.gob.reniec.certificados.cancelacion.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class FlywayIncrementalMigrationIT {

	@Container
	static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.0")
			.withDatabaseName("cancelacion_incremental_test");

	@Test
	void upgradesV4ToV5PreservingDataAndSeparatingAvailabilityFromListing() throws Exception {
		Flyway v4 = Flyway.configure()
				.dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
				.target(MigrationVersion.fromVersion("4"))
				.load();
		assertThat(v4.migrate().migrationsExecuted).isEqualTo(4);

		insertRepresentativeV4Data();
		List<String> retainedBefore = legacyRows();

		Flyway latest = Flyway.configure()
				.dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
				.load();
		assertThat(latest.migrate().migrationsExecuted).isEqualTo(1);

		assertThat(currentRows()).containsExactlyElementsOf(retainedBefore);
		assertThat(singleInt("""
				SELECT COUNT(*) FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				""")).isEqualTo(7);
		assertThat(singleInt("""
				SELECT COUNT(*) FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				""")).isEqualTo(80);
		assertThat(singleInt("""
				SELECT COUNT(*) FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name = 'certificate_availability_check'
				""")).isEqualTo(1);
		assertThat(singleInt("""
				SELECT COUNT(*) FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name = 'certificate_eligibility_check'
				""")).isZero();
		assertThat(singleInt("""
				SELECT COUNT(*) FROM information_schema.columns
				WHERE table_schema = DATABASE()
				  AND table_name = 'cancellation_request_certificate'
				  AND column_name = 'eligibility_check_id'
				""")).isZero();
		assertThat(schemaRows("""
				SELECT availability_result, request_status
				FROM certificate_cancellation_request WHERE id = 1
				""")).containsExactly("AVAILABLE|PENDING_IDENTITY_VERIFICATION");
		assertThat(schemaRows("""
				SELECT normalized_result FROM certificate_availability_check WHERE id = 1
				""")).containsExactly("AVAILABLE");
		assertThat(schemaRows("""
				SELECT CONCAT(table_name, '.', column_name)
				FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				  AND TRIM(COALESCE(column_comment, '')) = ''
				ORDER BY table_name, ordinal_position
				""")).isEmpty();
		assertThat(schemaRows("""
				SELECT table_name FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				  AND TRIM(COALESCE(table_comment, '')) = ''
				ORDER BY table_name
				""")).isEmpty();
	}

	private void insertRepresentativeV4Data() throws Exception {
		try (var connection = DriverManager.getConnection(
				MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
				var statement = connection.createStatement()) {
			statement.executeUpdate("""
					INSERT INTO certificate_cancellation_request
					(id, dni, request_status, eligibility_result, reason_code, confirmed_at,
					 final_outcome, created_at, updated_at)
					VALUES (1, '87654321', 'ELIGIBLE', 'ELIGIBLE', 'THEFT',
					 '2026-07-20 12:05:00', NULL, '2026-07-20 12:00:00', '2026-07-20 12:05:00')
					""");
			statement.executeUpdate("""
					INSERT INTO certificate_eligibility_check
					(id, request_id, attempt_number, check_status, normalized_result, external_reference,
					 requested_at, responded_at, correlation_id, created_at)
					VALUES (1, 1, 1, 'COMPLETED', 'ELIGIBLE', 'availability-v4',
					 '2026-07-20 12:00:00', '2026-07-20 12:00:01', 'corr-v4', '2026-07-20 12:00:00')
					""");
			statement.executeUpdate("""
					INSERT INTO cancellation_request_certificate
					(id, request_id, eligibility_check_id, order_number, emission_created_at,
					 certificate_uuid, availability_status, consulted_at, selected, selected_at,
					 version, created_at, updated_at)
					VALUES (1, 1, 1, 'ORD-V4-001', '2026-07-19 10:00:00',
					 '3ff0c799-5845-4c30-bb3d-f5ea260dad61', 'AVAILABLE',
					 '2026-07-20 12:00:01', TRUE, '2026-07-20 12:04:00', 2,
					 '2026-07-20 12:00:01', '2026-07-20 12:04:00')
					""");
		}
	}

	private List<String> legacyRows() throws Exception {
		return schemaRows("""
				SELECT CONCAT('request:', id, ':', dni) FROM certificate_cancellation_request
				UNION ALL SELECT CONCAT('check:', id, ':', external_reference) FROM certificate_eligibility_check
				UNION ALL SELECT CONCAT('certificate:', id, ':', order_number, ':', selected, ':', version) FROM cancellation_request_certificate
				ORDER BY 1
				""");
	}

	private List<String> currentRows() throws Exception {
		return schemaRows("""
				SELECT CONCAT('request:', id, ':', dni) FROM certificate_cancellation_request
				UNION ALL SELECT CONCAT('check:', id, ':', external_reference) FROM certificate_availability_check
				UNION ALL SELECT CONCAT('certificate:', id, ':', order_number, ':', selected, ':', version) FROM cancellation_request_certificate
				ORDER BY 1
				""");
	}

	private int singleInt(String sql) throws Exception {
		try (var connection = DriverManager.getConnection(
				MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
				var statement = connection.createStatement();
				var result = statement.executeQuery(sql)) {
			result.next();
			return result.getInt(1);
		}
	}

	private List<String> schemaRows(String sql) throws Exception {
		try (var connection = DriverManager.getConnection(
				MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
				var statement = connection.createStatement();
				var result = statement.executeQuery(sql)) {
			var metadata = result.getMetaData();
			List<String> rows = new ArrayList<>();
			while (result.next()) {
				StringBuilder row = new StringBuilder();
				for (int column = 1; column <= metadata.getColumnCount(); column++) {
					if (column > 1) row.append('|');
					row.append(result.getString(column));
				}
				rows.add(row.toString());
			}
			return rows;
		}
	}
}
