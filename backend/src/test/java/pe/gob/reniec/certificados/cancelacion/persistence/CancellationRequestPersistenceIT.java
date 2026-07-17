package pe.gob.reniec.certificados.cancelacion.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.AuditEventOrigin;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationAuditEventEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationAuditEventRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationAuditEventType;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationFinalOutcome;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReasonCode;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReceiptEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReceiptRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateEligibilityCheckEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateEligibilityCheckRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CurrentEligibilityResult;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.EligibilityCheckResult;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.EligibilityCheckStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.IdentityMatchResult;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.IdentityVerificationEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.IdentityVerificationRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.IdentityVerificationStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.ReceiptGenerationStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationResult;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "debug=false")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CancellationRequestPersistenceIT extends MySqlContainerSupport {

	private static final String CORRELATION = "persistence-test-correlation";

	@Autowired CertificateCancellationRequestRepository requestRepository;
	@Autowired CertificateEligibilityCheckRepository eligibilityRepository;
	@Autowired IdentityVerificationRepository identityRepository;
	@Autowired RevocationOperationRepository revocationRepository;
	@Autowired CancellationReceiptRepository receiptRepository;
	@Autowired CancellationAuditEventRepository auditRepository;
	@Autowired JdbcTemplate jdbcTemplate;

	@LocalServerPort int port;

	@BeforeEach
	void cleanTables() {
		jdbcTemplate.update("DELETE FROM cancellation_audit_event");
		jdbcTemplate.update("DELETE FROM cancellation_receipt");
		jdbcTemplate.update("DELETE FROM revocation_operation");
		jdbcTemplate.update("DELETE FROM identity_verification");
		jdbcTemplate.update("DELETE FROM certificate_eligibility_check");
		jdbcTemplate.update("DELETE FROM certificate_cancellation_request");
	}

	@Test
	void cleanDatabaseRunsFlywayCreatesOnlyTheSixSimplifiedTablesAndReportsSafeHealth() throws Exception {
		List<String> tables = jdbcTemplate.queryForList("""
				SELECT table_name FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				ORDER BY table_name
				""", String.class);
		List<String> obsoleteColumns = jdbcTemplate.queryForList("""
				SELECT column_name FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				AND column_name IN (
				 'dni_lookup_hash', 'dni_ciphertext', 'dni_key_version', 'dni_last_four',
				 'other_reason_ciphertext', 'other_reason_key_version', 'lifecycle_status',
				 'active_dni_guard', 'verified_identity_hash', 'session_reference_hash',
				 'token_family_id', 'client_reference_hash', 'open_request_guard',
				 'next_status_check_at', 'document_hash', 'template_version',
				 'technical_code', 'technical_detail', 'public_reference', 'consent_version',
				 'recoverable_until', 'expires_at', 'version', 'session_reference')
				""", String.class);
		Integer migrationCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE", Integer.class);
		HttpResponse<String> health = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health")).GET().build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(tables).containsExactly(
				"cancellation_audit_event", "cancellation_receipt",
				"certificate_cancellation_request", "certificate_eligibility_check",
				"identity_verification", "revocation_operation");
		assertThat(obsoleteColumns).isEmpty();
		assertThat(migrationCount).isEqualTo(1);
		assertThat(health.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(health.body()).contains("\"status\":\"UP\"")
				.doesNotContain("jdbc", "mysql", "username", "password", "sql", "dni");
	}

	@Test
	void initiatesEligibilityThroughHttpPersistsItAndRecoversConcurrentEligibleRequest() throws Exception {
		String body = "{\"dni\":\"00000001\"}";
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> { start.await(); return postCancellation(body); });
			var second = executor.submit(() -> { start.await(); return postCancellation(body); });
			start.countDown();
			HttpResponse<String> firstResponse = first.get();
			HttpResponse<String> secondResponse = second.get();
			assertThat(List.of(firstResponse.statusCode(), secondResponse.statusCode()))
					.allMatch(status -> status == 200 || status == 409);
			assertThat(firstResponse.body() + secondResponse.body())
					.contains("ELIGIBLE", "IDENTITY_VERIFICATION", "******01")
					.doesNotContain("00000001", "\"id\"");
		}
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM certificate_cancellation_request WHERE dni='00000001'", Integer.class))
				.isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM certificate_eligibility_check", Integer.class)).isEqualTo(1);

		jdbcTemplate.update("UPDATE certificate_cancellation_request SET created_at = '2000-01-01 00:00:00' WHERE dni='00000001'");
		HttpResponse<String> recovered = postCancellation(body);
		assertThat(recovered.statusCode()).isEqualTo(200);
		assertThat(recovered.body()).contains("\"reused\":true", "\"requestId\":")
				.doesNotContain("requestReference", "00000001");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM certificate_cancellation_request WHERE dni='00000001'", Integer.class))
				.isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM certificate_eligibility_check", Integer.class)).isEqualTo(1);
	}

	@Test
	void rejectsInvalidDniWithoutPersistenceAndReturnsCorrelation() throws Exception {
		HttpResponse<String> response = postCancellation("{\"dni\":\"1234\"}");
		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("VALIDATION_ERROR", "correlationId")
				.doesNotContain("1234");
		assertThat(requestRepository.count()).isZero();
		assertThat(eligibilityRepository.count()).isZero();
	}

	@Test
	void persistsEveryDeterministicAlternativeOutcomeWithControlledHttpSemantics() throws Exception {
		HttpResponse<String> notEligible = postCancellation("{\"dni\":\"00000002\"}");
		HttpResponse<String> unavailable = postCancellation("{\"dni\":\"00000003\"}");
		HttpResponse<String> inconclusive = postCancellation("{\"dni\":\"00000004\"}");
		HttpResponse<String> technical = postCancellation("{\"dni\":\"00000005\"}");
		HttpResponse<String> timeout = postCancellation("{\"dni\":\"00000006\"}");

		assertThat(notEligible.statusCode()).isEqualTo(200);
		assertThat(notEligible.body()).contains("NOT_ELIGIBLE").doesNotContain("00000002");
		assertThat(unavailable.statusCode()).isEqualTo(503);
		assertThat(unavailable.body()).contains("ELIGIBILITY_UNAVAILABLE").doesNotContain("00000003");
		assertThat(inconclusive.statusCode()).isEqualTo(200);
		assertThat(inconclusive.body()).contains("INCONCLUSIVE").doesNotContain("00000004");
		assertThat(technical.statusCode()).isEqualTo(502);
		assertThat(technical.body()).contains("ELIGIBILITY_PROVIDER_ERROR").doesNotContain("00000005");
		assertThat(timeout.statusCode()).isEqualTo(504);
		assertThat(timeout.body()).contains("ELIGIBILITY_TIMEOUT").doesNotContain("00000006");
		assertThat(requestRepository.count()).isEqualTo(5);
		assertThat(eligibilityRepository.count()).isEqualTo(5);
	}

	@Test
	void storesReadableRequestReasonConfirmationAndTerminalHistoryWithoutExpiration() {
		CertificateCancellationRequestEntity request = saveRequest("12345678");
		request.recordEligibility(CurrentEligibilityResult.ELIGIBLE, CancellationRequestStatus.ELIGIBLE);
		request.registerReason(CancellationReasonCode.OTHER, "Cambio de dispositivo personal");
		request.confirm(Instant.now());
		requestRepository.saveAndFlush(request);

		assertThat(requestRepository.findById(request.getId())).get().satisfies(found -> {
			assertThat(found.getDni()).isEqualTo("12345678");
			assertThat(found.getRequestStatus()).isEqualTo(CancellationRequestStatus.CONFIRMED);
			assertThat(found.getEligibilityResult()).isEqualTo(CurrentEligibilityResult.ELIGIBLE);
			assertThat(found.getReasonCode()).isEqualTo(CancellationReasonCode.OTHER);
			assertThat(found.getOtherReason()).isEqualTo("Cambio de dispositivo personal");
			assertThat(found.getConfirmedAt()).isNotNull();
		});
		assertThatThrownBy(() -> request.registerReason(CancellationReasonCode.LOSS, null))
				.isInstanceOf(IllegalStateException.class);
		assertThat(requestRepository.findFirstByDniAndRequestStatusInOrderByCreatedAtDesc(
				"12345678", Set.of(CancellationRequestStatus.CONFIRMED))).get()
				.extracting(CertificateCancellationRequestEntity::getId).isEqualTo(request.getId());
		assertThat(requestRepository.findFirstByDniOrderByCreatedAtDesc("12345678")).get()
				.extracting(CertificateCancellationRequestEntity::getId).isEqualTo(request.getId());

		CertificateCancellationRequestEntity historical = saveRequest("12345678");
		historical.transitionTo(CancellationRequestStatus.ABANDONED, CancellationFinalOutcome.ABANDONED);
		requestRepository.saveAndFlush(historical);
		assertThat(requestRepository.count()).isEqualTo(2);
		assertThatThrownBy(() -> new CertificateCancellationRequestEntity("1234ABCD"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void recordsRepeatableEligibilityAndIdentityAttemptsAndFindsLatestValid() {
		CertificateCancellationRequestEntity request = saveRequest("23456789");
		Instant now = Instant.now();
		CertificateEligibilityCheckEntity eligibility1 = new CertificateEligibilityCheckEntity(
				request, 1, EligibilityCheckStatus.SUBMITTED, now, CORRELATION);
		eligibility1.complete(EligibilityCheckResult.INCONCLUSIVE, now.plusSeconds(1), "eligibility-ref-1");
		eligibilityRepository.saveAndFlush(eligibility1);
		CertificateEligibilityCheckEntity eligibility2 = new CertificateEligibilityCheckEntity(
				request, 2, EligibilityCheckStatus.SUBMITTED, now.plusSeconds(2), CORRELATION);
		eligibility2.complete(EligibilityCheckResult.ELIGIBLE, now.plusSeconds(3), "eligibility-ref-2");
		eligibilityRepository.saveAndFlush(eligibility2);

		IdentityVerificationEntity identity1 = new IdentityVerificationEntity(request, 1, "ID_PERU", now, CORRELATION);
		identity1.finish(IdentityVerificationStatus.REJECTED, IdentityMatchResult.INCONCLUSIVE,
				now.plusSeconds(1), "identity-ref-1", "REJECTED_BY_PROVIDER");
		identityRepository.saveAndFlush(identity1);
		IdentityVerificationEntity identity2 = new IdentityVerificationEntity(
				request, 2, "ID_PERU", now.plusSeconds(2), CORRELATION);
		identity2.finish(IdentityVerificationStatus.VERIFIED, IdentityMatchResult.MATCH,
				now.plusSeconds(3), "identity-ref-2", null);
		identityRepository.saveAndFlush(identity2);

		assertThat(eligibilityRepository.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId())).get()
				.extracting(CertificateEligibilityCheckEntity::getId).isEqualTo(eligibility2.getId());
		assertThat(identityRepository.findFirstByRequest_IdAndVerificationStatusOrderByAttemptNumberDesc(
				request.getId(), IdentityVerificationStatus.VERIFIED)).get()
				.extracting(IdentityVerificationEntity::getId).isEqualTo(identity2.getId());
		assertThatThrownBy(() -> eligibilityRepository.saveAndFlush(new CertificateEligibilityCheckEntity(
				request, 2, EligibilityCheckStatus.CREATED, now.plusSeconds(4), CORRELATION)))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> identityRepository.saveAndFlush(new IdentityVerificationEntity(
				request, 2, "ID_PERU", now.plusSeconds(4), CORRELATION)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void keepsUncertainRevocationOnTheSameOperationAndEnforcesIdempotencyAndAttemptUniqueness() {
		CertificateCancellationRequestEntity request = saveRequest("45678901");
		String key = "revoke-request-001";
		Instant now = Instant.now();
		RevocationOperationEntity uncertain = new RevocationOperationEntity(request, key, 1, now, CORRELATION);
		uncertain.markSubmitted(now.plusSeconds(1), "revocation-ref-1");
		uncertain.complete(RevocationOperationStatus.OUTCOME_UNKNOWN, RevocationResult.OUTCOME_UNKNOWN,
				now.plusSeconds(2), null, "PROVIDER_TIMEOUT");
		revocationRepository.saveAndFlush(uncertain);

		assertThat(revocationRepository.findByIdempotencyKey(key)).get()
				.extracting(RevocationOperationEntity::getId).isEqualTo(uncertain.getId());
		assertThat(revocationRepository.findFirstByRequest_IdAndOperationStatusInOrderByAttemptNumberDesc(
				request.getId(), Set.of(RevocationOperationStatus.OUTCOME_UNKNOWN))).get()
				.extracting(RevocationOperationEntity::getId).isEqualTo(uncertain.getId());
		assertThatThrownBy(() -> revocationRepository.saveAndFlush(
				new RevocationOperationEntity(request, "revoke-request-002", 1, now.plusSeconds(3), CORRELATION)))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> revocationRepository.saveAndFlush(
				new RevocationOperationEntity(request, key, 2, now.plusSeconds(4), CORRELATION)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void associatesReceiptsWithSuccessfulRevocationAndKeepsReceiptFailureIndependent() {
		CertificateCancellationRequestEntity request = saveRequest("56789012");
		Instant now = Instant.now();
		RevocationOperationEntity operation = new RevocationOperationEntity(
				request, "revoke-receipt-001", 1, now, CORRELATION);
		operation.complete(RevocationOperationStatus.SUCCEEDED, RevocationResult.SUCCEEDED,
				now.plusSeconds(1), now.plusSeconds(1), null);
		revocationRepository.saveAndFlush(operation);
		request.transitionTo(CancellationRequestStatus.COMPLETED, CancellationFinalOutcome.REVOCATION_SUCCEEDED);
		requestRepository.saveAndFlush(request);

		CancellationReceiptEntity available = new CancellationReceiptEntity(request, operation, "CD-TEST-0001");
		available.markAvailable("documents/receipt-0001", now.plusSeconds(2), now.plusSeconds(3));
		receiptRepository.saveAndFlush(available);
		CancellationReceiptEntity failed = new CancellationReceiptEntity(request, operation, "CD-TEST-0002");
		failed.markFailed("RENDER_FAILURE");
		receiptRepository.saveAndFlush(failed);

		assertThat(receiptRepository.findFirstByRequest_IdAndGenerationStatusOrderByAvailableAtDesc(
				request.getId(), ReceiptGenerationStatus.AVAILABLE)).get()
				.extracting(CancellationReceiptEntity::getId).isEqualTo(available.getId());
		assertThat(requestRepository.findById(request.getId())).get()
				.extracting(CertificateCancellationRequestEntity::getFinalOutcome)
				.isEqualTo(CancellationFinalOutcome.REVOCATION_SUCCEEDED);
		assertThatThrownBy(() -> receiptRepository.saveAndFlush(
				new CancellationReceiptEntity(request, operation, "CD-TEST-0001")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void appendsOrderedMinimalAuditHistoryAndRejectsOrphanRows() {
		CertificateCancellationRequestEntity request = saveRequest("67890123");
		Instant now = Instant.now();
		CancellationAuditEventEntity started = auditRepository.save(new CancellationAuditEventEntity(
				request, CancellationAuditEventType.REQUEST_STARTED, null, CancellationRequestStatus.STARTED,
				"CREATED", CORRELATION, AuditEventOrigin.CITIZEN, now));
		CancellationAuditEventEntity checked = auditRepository.save(new CancellationAuditEventEntity(
				request, CancellationAuditEventType.ELIGIBILITY_CHECKED, CancellationRequestStatus.STARTED,
				CancellationRequestStatus.ELIGIBLE, "ELIGIBLE", CORRELATION,
				AuditEventOrigin.EXTERNAL_PROVIDER, now.plusSeconds(1)));

		assertThat(auditRepository.findByRequest_IdOrderByOccurredAtAscIdAsc(request.getId()))
				.extracting(CancellationAuditEventEntity::getId).containsExactly(started.getId(), checked.getId());
		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO cancellation_audit_event
				(request_id, event_type, correlation_id, event_origin, occurred_at)
				VALUES (?, 'REQUEST_STARTED', ?, 'SYSTEM', CURRENT_TIMESTAMP(6))
				""", Long.MAX_VALUE, CORRELATION)).isInstanceOf(DataIntegrityViolationException.class);
	}

	private CertificateCancellationRequestEntity saveRequest(String dni) {
		return requestRepository.saveAndFlush(new CertificateCancellationRequestEntity(dni));
	}

	private HttpResponse<String> postCancellation(String body) throws Exception {
		return HttpClient.newHttpClient().send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/api/v1/cancellation-requests"))
				.header("Content-Type", "application/json")
				.header("X-Correlation-ID", "eligibility-it")
				.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
	}
}
