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
	void upgradesV3ToV4PreservingAtomicModelDataAndRemovingOnlyIndividualResults() throws Exception {
		Flyway v3 = Flyway.configure()
				.dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
				.target(MigrationVersion.fromVersion("3"))
				.load();
		assertThat(v3.migrate().migrationsExecuted).isEqualTo(3);

		insertRepresentativeV3Data();
		List<String> retainedBefore = retainedRows();

		Flyway latest = Flyway.configure()
				.dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
				.load();
		assertThat(latest.migrate().migrationsExecuted).isEqualTo(1);

		assertThat(retainedRows()).containsExactlyElementsOf(retainedBefore);
		assertThat(singleInt("""
				SELECT COUNT(*) FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				""")).isEqualTo(7);
		assertThat(singleInt("""
				SELECT COUNT(*) FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				""")).isEqualTo(81);
		assertThat(singleInt("""
				SELECT COUNT(*) FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name = 'certificate_revocation_result'
				""")).isZero();
		assertThat(schemaRows("""
				SELECT index_name FROM information_schema.statistics
				WHERE table_schema = DATABASE()
				AND index_name IN ('uq_request_certificate_identity', 'uq_revocation_request_identity')
				ORDER BY index_name
				""")).isEmpty();
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

	private void insertRepresentativeV3Data() throws Exception {
		try (var connection = DriverManager.getConnection(
				MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
				var statement = connection.createStatement()) {
			statement.executeUpdate("""
					INSERT INTO certificate_cancellation_request
					(id, dni, request_status, eligibility_result, reason_code, confirmed_at,
					 final_outcome, created_at, updated_at)
					VALUES (1, '87654321', 'CONFIRMED', 'ELIGIBLE', 'THEFT',
					 '2026-07-20 12:05:00', NULL, '2026-07-20 12:00:00', '2026-07-20 12:05:00')
					""");
			statement.executeUpdate("""
					INSERT INTO certificate_eligibility_check
					(id, request_id, attempt_number, check_status, normalized_result, external_reference,
					 requested_at, responded_at, correlation_id, created_at)
					VALUES (1, 1, 1, 'COMPLETED', 'ELIGIBLE', 'eligibility-v3',
					 '2026-07-20 12:00:00', '2026-07-20 12:00:01', 'corr-v3', '2026-07-20 12:00:00')
					""");
			statement.executeUpdate("""
					INSERT INTO cancellation_request_certificate
					(id, request_id, eligibility_check_id, order_number, emission_created_at,
					 certificate_uuid, availability_status, consulted_at, selected, selected_at,
					 version, created_at, updated_at)
					VALUES (1, 1, 1, 'ORD-V3-001', '2026-07-19 10:00:00',
					 '3ff0c799-5845-4c30-bb3d-f5ea260dad61', 'REVOCATION_PENDING',
					 '2026-07-20 12:00:01', TRUE, '2026-07-20 12:04:00', 2,
					 '2026-07-20 12:00:01', '2026-07-20 12:04:00')
					""");
			statement.executeUpdate("""
					INSERT INTO revocation_operation
					(id, request_id, idempotency_key, attempt_number, operation_status, external_reference,
					 prepared_at, submitted_at, responded_at, completed_at, normalized_result,
					 correlation_id, created_at, updated_at)
					VALUES (1, 1, 'idem-v3-001', 1, 'SUCCEEDED', 'revocation-v3',
					 '2026-07-20 12:06:00', '2026-07-20 12:06:01', '2026-07-20 12:06:02',
					 '2026-07-20 12:06:02', 'SUCCEEDED', 'corr-v3',
					 '2026-07-20 12:06:00', '2026-07-20 12:06:02')
					""");
			statement.executeUpdate("""
					INSERT INTO cancellation_receipt
					(id, request_id, revocation_operation_id, receipt_code, generation_status,
					 storage_reference, generated_at, available_at, created_at, updated_at)
					VALUES (1, 1, 1, 'CD-V3-001', 'AVAILABLE', 'receipts/v3-001.pdf',
					 '2026-07-20 12:07:00', '2026-07-20 12:07:01',
					 '2026-07-20 12:07:00', '2026-07-20 12:07:01')
					""");
			statement.executeUpdate("""
					INSERT INTO cancellation_audit_event
					(id, request_id, event_type, previous_status, new_status, result,
					 correlation_id, event_origin, occurred_at)
					VALUES (1, 1, 'REVOCATION_CONFIRMED', 'REVOCATION_IN_PROGRESS',
					 'REVOCATION_SUCCEEDED', 'SUCCEEDED', 'corr-v3', 'SYSTEM', '2026-07-20 12:06:02')
					""");
		}
	}

	private List<String> retainedRows() throws Exception {
		return schemaRows("""
				SELECT CONCAT('request:', id, ':', dni, ':', request_status) FROM certificate_cancellation_request
				UNION ALL SELECT CONCAT('eligibility:', id, ':', external_reference) FROM certificate_eligibility_check
				UNION ALL SELECT CONCAT('certificate:', id, ':', order_number, ':', selected, ':', version) FROM cancellation_request_certificate
				UNION ALL SELECT CONCAT('operation:', id, ':', idempotency_key, ':', normalized_result) FROM revocation_operation
				UNION ALL SELECT CONCAT('receipt:', id, ':', receipt_code, ':', generation_status) FROM cancellation_receipt
				UNION ALL SELECT CONCAT('audit:', id, ':', event_type, ':', result) FROM cancellation_audit_event
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
