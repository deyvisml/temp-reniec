package pe.gob.reniec.credenciales.revocacion.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
			.withDatabaseName("revocacion_incremental_test");

	@Container
	static final MySQLContainer CONFLICT_MYSQL = new MySQLContainer("mysql:8.4.0")
			.withDatabaseName("revocacion_conflict_test");

	@Container
	static final MySQLContainer DRAFT_MYSQL = new MySQLContainer("mysql:8.4.0")
			.withDatabaseName("revocacion_draft_test");

	@Test
	void upgradesV4ToLatestPreservingDataAndAddingCurrentSchemaChanges() throws Exception {
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
		assertThat(latest.migrate().migrationsExecuted).isEqualTo(11);

		assertThat(currentRows()).containsExactlyElementsOf(retainedBefore);
		assertThat(singleInt("""
				SELECT COUNT(*) FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				""")).isEqualTo(8);
		assertThat(singleInt("""
				SELECT COUNT(*) FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				""")).isEqualTo(110);
		assertThat(schemaRows("""
				SELECT CONCAT(column_name, '|', is_nullable, '|', column_comment)
				FROM information_schema.columns
				WHERE table_schema = DATABASE()
				  AND table_name = 'revocation_request_digital_credential'
				  AND column_name IN ('legacy_order_number', 'status_list_index',
				      'credential_type', 'provider_credential_status')
				ORDER BY ordinal_position
				""")).containsExactly(
				"legacy_order_number|YES|Orden histórica del proveedor anterior; no se usa para nuevas revocaciones",
				"status_list_index|YES|Índice oficial e inmutable de la credencial en la lista del proveedor",
				"credential_type|YES|Tipo de credencial informado por el proveedor oficial",
				"provider_credential_status|YES|Estado crudo validado del proveedor: 0 vigente, 1 revocada");
		assertThat(schemaRows("""
				SELECT CONCAT(is_nullable, '|', character_maximum_length, '|', column_comment)
				FROM information_schema.columns
				WHERE table_schema = DATABASE()
				  AND table_name = 'digital_credential_revocation_request'
				  AND column_name = 'consent_version'
				""")).containsExactly("YES|64|Versión del texto de consentimiento aceptado por el ciudadano");
		assertThat(singleInt("""
				SELECT COUNT(*) FROM digital_credential_revocation_request
				WHERE id = 1 AND confirmed_at IS NOT NULL AND consent_version IS NULL
				""")).isEqualTo(1);
		assertThat(schemaRows("""
				SELECT column_name FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name = 'identity_verification'
				  AND column_name IN ('provider_mode', 'state_hash', 'state_expires_at', 'state_consumed_at',
				    'pkce_verifier_protected', 'verified_subject_hash', 'verified_first_name')
				ORDER BY column_name
				""")).containsExactly("pkce_verifier_protected", "provider_mode", "state_consumed_at",
				"state_expires_at", "state_hash", "verified_first_name", "verified_subject_hash");
		assertThat(singleInt("""
				SELECT COUNT(*) FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name = 'digital_credential_availability_check'
				""")).isEqualTo(1);
		assertThat(singleInt("""
				SELECT COUNT(*) FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name = 'certificate_eligibility_check'
				""")).isZero();
		assertThat(singleInt("""
				SELECT COUNT(*) FROM information_schema.columns
				WHERE table_schema = DATABASE()
				  AND table_name = 'revocation_request_digital_credential'
				  AND column_name = 'eligibility_check_id'
				""")).isZero();
		assertThat(schemaRows("""
				SELECT availability_result, request_status
				FROM digital_credential_revocation_request WHERE id = 1
				""")).containsExactly("AVAILABLE|PENDING_IDENTITY_VERIFICATION");
		assertThat(schemaRows("""
				SELECT normalized_result FROM digital_credential_availability_check WHERE id = 1
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
		assertThat(singleInt("""
				SELECT COUNT(*) FROM information_schema.statistics
				WHERE table_schema = DATABASE()
				  AND table_name = 'revocation_request_digital_credential'
				  AND index_name = 'uq_revocation_request_single_selected'
				""")).isEqualTo(1);
	}

	@Test
	void rejectsLegacyMultipleSelectionsWithoutChangingTheirEvidence() throws Exception {
		Flyway v4 = Flyway.configure()
				.dataSource(CONFLICT_MYSQL.getJdbcUrl(), CONFLICT_MYSQL.getUsername(), CONFLICT_MYSQL.getPassword())
				.target(MigrationVersion.fromVersion("4"))
				.load();
		v4.migrate();
		insertConflictingV4Data();

		Flyway latest = Flyway.configure()
				.dataSource(CONFLICT_MYSQL.getJdbcUrl(), CONFLICT_MYSQL.getUsername(), CONFLICT_MYSQL.getPassword())
				.load();
		assertThatThrownBy(latest::migrate).hasMessageContaining("uq_request_certificate_single_selected");
		assertThat(singleInt(CONFLICT_MYSQL, """
				SELECT COUNT(*) FROM cancellation_request_certificate
				WHERE request_id = 1 AND selected = TRUE
				""")).isEqualTo(2);
	}

	@Test
	void clearsOnlyUnconfirmedDraftEvidenceWhenUpgradingV9() throws Exception {
		Flyway v9 = Flyway.configure()
				.dataSource(DRAFT_MYSQL.getJdbcUrl(), DRAFT_MYSQL.getUsername(), DRAFT_MYSQL.getPassword())
				.target(MigrationVersion.fromVersion("9"))
				.load();
		v9.migrate();
		try (var connection = DriverManager.getConnection(
				DRAFT_MYSQL.getJdbcUrl(), DRAFT_MYSQL.getUsername(), DRAFT_MYSQL.getPassword());
				var statement = connection.createStatement()) {
			statement.executeUpdate("""
					INSERT INTO certificate_cancellation_request
					(id, dni, request_status, availability_result, reason_code, other_reason,
					 confirmed_at, consent_version, created_at, updated_at)
					VALUES
					(1, '87654321', 'REASON_REGISTERED', 'AVAILABLE', 'OTHER', 'Borrador no confirmado',
					 NULL, NULL, '2026-07-20 12:00:00', '2026-07-20 12:05:00'),
					(2, '12345678', 'CONFIRMED', 'AVAILABLE', 'LOSS', NULL,
					 '2026-07-20 12:06:00', 'CANCELACION_CERTIFICADOS_V1',
					 '2026-07-20 12:00:00', '2026-07-20 12:06:00')
					""");
			statement.executeUpdate("""
					INSERT INTO cancellation_request_certificate
					(id, request_id, order_number, emission_created_at, certificate_uuid,
					 availability_status, consulted_at, selected, selected_at, version, created_at, updated_at)
					VALUES
					(1, 1, 'ORD-DRAFT', '2026-07-19 10:00:00',
					 '3ff0c799-5845-4c30-bb3d-f5ea260dad61', 'AVAILABLE',
					 '2026-07-20 12:00:01', TRUE, '2026-07-20 12:04:00', 1,
					 '2026-07-20 12:00:01', '2026-07-20 12:04:00'),
					(2, 2, 'ORD-CONFIRMED', '2026-07-19 11:00:00',
					 '31ab4d38-e7ef-47af-af8c-f7fedc05a1d2', 'REVOKED',
					 '2026-07-20 12:00:01', TRUE, '2026-07-20 12:06:00', 1,
					 '2026-07-20 12:00:01', '2026-07-20 12:06:00')
					""");
			statement.executeUpdate("""
					INSERT INTO revocation_operation
					(id, request_id, idempotency_key, attempt_number, operation_status,
					 prepared_at, submitted_at, responded_at, completed_at, normalized_result,
					 correlation_id, created_at, updated_at)
					VALUES
					(1, 2, 'legacy-revocation-2', 1, 'SUCCEEDED',
					 '2026-07-20 12:06:01', '2026-07-20 12:06:02', '2026-07-20 12:06:03',
					 '2026-07-20 12:06:03', 'SUCCEEDED', 'legacy-revocation',
					 '2026-07-20 12:06:01', '2026-07-20 12:06:03')
					""");
			statement.executeUpdate("""
					INSERT INTO identity_verification
					(id, request_id, attempt_number, provider, provider_mode, verification_status,
					 external_reference, verified_subject_hash, dni_match_result, started_at,
					 completed_at, correlation_id, created_at, updated_at)
					VALUES
					(1, 1, 1, 'ID_PERU', 'REAL', 'VERIFIED', 'legacy-identity',
					 REPEAT('a', 64), 'MATCH', '2026-07-20 12:01:00',
					 '2026-07-20 12:02:00', 'legacy-identity-correlation',
					 '2026-07-20 12:01:00', '2026-07-20 12:02:00')
					""");
			statement.executeUpdate("""
					INSERT INTO cancellation_flow_session
					(id, request_id, session_status, refresh_family, refresh_version,
					 current_refresh_hash, refresh_expires_at, last_used_at, created_at, updated_at, version)
					VALUES
					(1, 1, 'IDENTITY_VERIFIED', '11111111-1111-4111-8111-111111111111', 1,
					 REPEAT('b', 64), '2026-08-20 12:00:00', '2026-07-20 12:02:00',
					 '2026-07-20 12:00:00', '2026-07-20 12:02:00', 0)
					""");
		}

		Flyway latest = Flyway.configure()
				.dataSource(DRAFT_MYSQL.getJdbcUrl(), DRAFT_MYSQL.getUsername(), DRAFT_MYSQL.getPassword())
				.load();
		assertThat(latest.migrate().migrationsExecuted).isEqualTo(6);
		assertThat(singleInt(DRAFT_MYSQL, """
				SELECT COUNT(*) FROM digital_credential_revocation_request
				WHERE id = 1 AND request_status = 'PENDING_IDENTITY_VERIFICATION'
				  AND reason_code IS NULL AND other_reason IS NULL
				""")).isEqualTo(1);
		assertThat(singleInt(DRAFT_MYSQL, """
				SELECT COUNT(*) FROM revocation_flow_session
				WHERE request_id = 1 AND session_status = 'PENDING_IDENTITY'
				""")).isEqualTo(1);
		assertThat(singleInt(DRAFT_MYSQL, """
				SELECT COUNT(*) FROM revocation_request_digital_credential
				WHERE id = 1 AND selected = FALSE AND selected_at IS NULL
				""")).isEqualTo(1);
		assertThat(singleInt(DRAFT_MYSQL, """
				SELECT COUNT(*) FROM digital_credential_revocation_request request
				JOIN revocation_request_digital_credential digitalCredential ON digitalCredential.request_id = request.id
				WHERE request.id = 2 AND request.request_status = 'CONFIRMED'
				  AND request.reason_code = 'LOSS' AND request.confirmed_at IS NOT NULL
				  AND digitalCredential.selected = TRUE AND digitalCredential.selected_at IS NOT NULL
				  AND digitalCredential.availability_status = 'REVOKED'
				  AND digitalCredential.revoked_at = '2026-07-20 12:06:03'
				""")).isEqualTo(1);
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

	private void insertConflictingV4Data() throws Exception {
		try (var connection = DriverManager.getConnection(
				CONFLICT_MYSQL.getJdbcUrl(), CONFLICT_MYSQL.getUsername(), CONFLICT_MYSQL.getPassword());
				var statement = connection.createStatement()) {
			statement.executeUpdate("""
					INSERT INTO certificate_cancellation_request
					(id, dni, request_status, eligibility_result, created_at, updated_at)
					VALUES (1, '87654321', 'ELIGIBLE', 'ELIGIBLE',
					 '2026-07-20 12:00:00', '2026-07-20 12:05:00')
					""");
			statement.executeUpdate("""
					INSERT INTO certificate_eligibility_check
					(id, request_id, attempt_number, check_status, normalized_result,
					 requested_at, responded_at, correlation_id, created_at)
					VALUES (1, 1, 1, 'COMPLETED', 'ELIGIBLE',
					 '2026-07-20 12:00:00', '2026-07-20 12:00:01', 'conflict-v4', '2026-07-20 12:00:00')
					""");
			statement.executeUpdate("""
					INSERT INTO cancellation_request_certificate
					(id, request_id, eligibility_check_id, order_number, emission_created_at,
					 certificate_uuid, availability_status, consulted_at, selected, selected_at,
					 version, created_at, updated_at)
					VALUES
					(1, 1, 1, 'ORD-V4-001', '2026-07-19 10:00:00',
					 '3ff0c799-5845-4c30-bb3d-f5ea260dad61', 'AVAILABLE',
					 '2026-07-20 12:00:01', TRUE, '2026-07-20 12:04:00', 1,
					 '2026-07-20 12:00:01', '2026-07-20 12:04:00'),
					(2, 1, 1, 'ORD-V4-002', '2026-07-19 11:00:00',
					 '31ab4d38-e7ef-47af-af8c-f7fedc05a1d2', 'AVAILABLE',
					 '2026-07-20 12:00:01', TRUE, '2026-07-20 12:04:01', 1,
					 '2026-07-20 12:00:01', '2026-07-20 12:04:01')
					""");
		}
	}

	private List<String> legacyRows() throws Exception {
		return schemaRows("""
				SELECT CONCAT('request:', id, ':', dni) FROM certificate_cancellation_request
				UNION ALL SELECT CONCAT('check:', id, ':', external_reference) FROM certificate_eligibility_check
				UNION ALL SELECT CONCAT('digitalCredential:', id, ':', order_number, ':', selected, ':', version) FROM cancellation_request_certificate
				ORDER BY 1
				""");
	}

	private List<String> currentRows() throws Exception {
		return schemaRows("""
				SELECT CONCAT('request:', id, ':', dni) FROM digital_credential_revocation_request
				UNION ALL SELECT CONCAT('check:', id, ':', external_reference) FROM digital_credential_availability_check
				UNION ALL SELECT CONCAT('digitalCredential:', id, ':', legacy_order_number, ':', selected, ':', version) FROM revocation_request_digital_credential
				ORDER BY 1
				""");
	}

	private int singleInt(String sql) throws Exception {
		return singleInt(MYSQL, sql);
	}

	private int singleInt(MySQLContainer container, String sql) throws Exception {
		try (var connection = DriverManager.getConnection(
				container.getJdbcUrl(), container.getUsername(), container.getPassword());
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
