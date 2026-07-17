package pe.gob.reniec.certificados.cancelacion.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
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
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestSessionEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestSessionRepository;
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

	private static final Duration LIFETIME = Duration.ofHours(4);
	private static final String CORRELATION = "persistence-test-correlation";

	@Autowired CertificateCancellationRequestRepository requestRepository;
	@Autowired CertificateEligibilityCheckRepository eligibilityRepository;
	@Autowired IdentityVerificationRepository identityRepository;
	@Autowired CancellationRequestSessionRepository sessionRepository;
	@Autowired RevocationOperationRepository revocationRepository;
	@Autowired CancellationReceiptRepository receiptRepository;
	@Autowired CancellationAuditEventRepository auditRepository;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired EntityManagerFactory entityManagerFactory;

	@LocalServerPort int port;

	@BeforeEach
	void cleanTables() {
		jdbcTemplate.update("DELETE FROM cancellation_audit_event");
		jdbcTemplate.update("DELETE FROM cancellation_receipt");
		jdbcTemplate.update("DELETE FROM revocation_operation");
		jdbcTemplate.update("DELETE FROM cancellation_request_session");
		jdbcTemplate.update("DELETE FROM identity_verification");
		jdbcTemplate.update("DELETE FROM certificate_eligibility_check");
		jdbcTemplate.update("DELETE FROM certificate_cancellation_request");
	}

	@Test
	void cleanDatabaseRunsFlywayCreatesOnlyTheSevenSimplifiedTablesAndReportsSafeHealth() throws Exception {
		List<String> tables = jdbcTemplate.queryForList("""
				SELECT table_name FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				ORDER BY table_name
				""", String.class);
		List<String> obsoleteColumns = jdbcTemplate.queryForList("""
				SELECT column_name FROM information_schema.columns
				WHERE table_schema = DATABASE() AND column_name IN (
				 'dni_lookup_hash', 'dni_ciphertext', 'dni_key_version', 'dni_last_four',
				 'other_reason_ciphertext', 'other_reason_key_version', 'lifecycle_status',
				 'active_dni_guard', 'verified_identity_hash', 'session_reference_hash',
				 'token_family_id', 'client_reference_hash', 'open_request_guard',
				 'next_status_check_at', 'document_hash', 'template_version',
				 'technical_code', 'technical_detail')
				""", String.class);
		Integer migrationCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE", Integer.class);
		HttpResponse<String> health = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health")).GET().build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(tables).containsExactly(
				"cancellation_audit_event", "cancellation_receipt", "cancellation_request_session",
				"certificate_cancellation_request", "certificate_eligibility_check",
				"identity_verification", "revocation_operation");
		assertThat(obsoleteColumns).isEmpty();
		assertThat(migrationCount).isEqualTo(1);
		assertThat(health.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(health.body()).contains("\"status\":\"UP\"")
				.doesNotContain("jdbc", "mysql", "username", "password", "sql", "dni");
	}

	@Test
	void storesReadableRequestReasonConsentHistoryExpirationAndOptimisticConcurrency() {
		Instant now = Instant.now();
		CertificateCancellationRequestEntity request = saveRequest("12345678", now.plus(LIFETIME));
		request.recordEligibility(CurrentEligibilityResult.ELIGIBLE, CancellationRequestStatus.ELIGIBLE);
		request.registerReason(CancellationReasonCode.OTHER, "Cambio de dispositivo personal");
		request.confirm("consent-v1", Instant.now());
		requestRepository.saveAndFlush(request);

		assertThat(requestRepository.findById(request.getId())).get().satisfies(found -> {
			assertThat(found.getDni()).isEqualTo("12345678");
			assertThat(found.getRequestStatus()).isEqualTo(CancellationRequestStatus.CONFIRMED);
			assertThat(found.getEligibilityResult()).isEqualTo(CurrentEligibilityResult.ELIGIBLE);
			assertThat(found.getReasonCode()).isEqualTo(CancellationReasonCode.OTHER);
			assertThat(found.getOtherReason()).isEqualTo("Cambio de dispositivo personal");
			assertThat(found.getConsentVersion()).isEqualTo("consent-v1");
		});
		assertThatThrownBy(() -> request.registerReason(CancellationReasonCode.LOSS, null))
				.isInstanceOf(IllegalStateException.class);
		assertThat(requestRepository.findFirstByDniAndRequestStatusInOrderByCreatedAtDesc(
				"12345678", Set.of(CancellationRequestStatus.CONFIRMED))).get()
				.extracting(CertificateCancellationRequestEntity::getId).isEqualTo(request.getId());
		assertThat(requestRepository.findFirstByDniOrderByCreatedAtDesc("12345678")).get()
				.extracting(CertificateCancellationRequestEntity::getId).isEqualTo(request.getId());

		CertificateCancellationRequestEntity historical = saveRequest("12345678", now.plusSeconds(30));
		historical.transitionTo(CancellationRequestStatus.ABANDONED, CancellationFinalOutcome.ABANDONED);
		requestRepository.saveAndFlush(historical);
		assertThat(requestRepository.count()).isEqualTo(2);
		assertThat(requestRepository.findByRequestStatusInAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
				Set.of(CancellationRequestStatus.ABANDONED), now.plusSeconds(60)))
				.extracting(CertificateCancellationRequestEntity::getId).containsExactly(historical.getId());

		assertOptimisticRequestLock(request.getId());
		assertThatThrownBy(() -> new CertificateCancellationRequestEntity(
				"1234ABCD", Instant.now().plusSeconds(10), Instant.now().plusSeconds(20)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void recordsRepeatableEligibilityAndIdentityAttemptsAndFindsLatestValid() {
		CertificateCancellationRequestEntity request = saveRequest("23456789", Instant.now().plus(LIFETIME));
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
	void storesMultipleSessionsAndReturnsOnlyActiveOnesWithoutTokenFields() {
		CertificateCancellationRequestEntity request = saveRequest("34567890", Instant.now().plus(LIFETIME));
		Instant now = Instant.now();
		CancellationRequestSessionEntity active = sessionRepository.saveAndFlush(
				new CancellationRequestSessionEntity(request, "session-local-001", now.plusSeconds(120)));
		CancellationRequestSessionEntity invalidated = sessionRepository.saveAndFlush(
				new CancellationRequestSessionEntity(request, "session-local-002", now.plusSeconds(120)));
		invalidated.invalidate(Instant.now(), "RECOVERED_ON_ANOTHER_DEVICE");
		sessionRepository.saveAndFlush(invalidated);
		CancellationRequestSessionEntity expired = sessionRepository.saveAndFlush(
				new CancellationRequestSessionEntity(request, "session-local-003", now.plusMillis(500)));

		assertThat(sessionRepository.findBySessionReference("session-local-001")).get()
				.extracting(CancellationRequestSessionEntity::getId).isEqualTo(active.getId());
		List<CancellationRequestSessionEntity> sessions = sessionRepository
				.findByRequest_IdAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
						request.getId(), now.plusSeconds(1));
		assertThat(sessions).extracting(CancellationRequestSessionEntity::getId)
				.containsExactly(active.getId()).doesNotContain(invalidated.getId(), expired.getId());
	}

	@Test
	void keepsUncertainRevocationOnTheSameOperationAndEnforcesIdempotencyAndAttemptUniqueness() {
		CertificateCancellationRequestEntity request = saveRequest("45678901", Instant.now().plus(LIFETIME));
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
		assertOptimisticRevocationLock(uncertain.getId());
	}

	@Test
	void associatesReceiptsWithSuccessfulRevocationAndKeepsReceiptFailureIndependent() {
		CertificateCancellationRequestEntity request = saveRequest("56789012", Instant.now().plus(LIFETIME));
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
		CertificateCancellationRequestEntity request = saveRequest("67890123", Instant.now().plus(LIFETIME));
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

	private CertificateCancellationRequestEntity saveRequest(String dni, Instant expiry) {
		return requestRepository.saveAndFlush(new CertificateCancellationRequestEntity(
				dni, Instant.now().plus(Duration.ofHours(2)), expiry));
	}

	private void assertOptimisticRequestLock(Long requestId) {
		EntityManager first = entityManagerFactory.createEntityManager();
		EntityManager second = entityManagerFactory.createEntityManager();
		try {
			first.getTransaction().begin();
			second.getTransaction().begin();
			CertificateCancellationRequestEntity firstCopy = first.find(CertificateCancellationRequestEntity.class, requestId);
			CertificateCancellationRequestEntity secondCopy = second.find(CertificateCancellationRequestEntity.class, requestId);
			firstCopy.recordEligibility(CurrentEligibilityResult.ELIGIBLE, CancellationRequestStatus.ELIGIBLE);
			first.getTransaction().commit();
			secondCopy.recordEligibility(CurrentEligibilityResult.INCONCLUSIVE, CancellationRequestStatus.CHECKING_ELIGIBILITY);
			assertThatThrownBy(second.getTransaction()::commit)
					.isInstanceOfAny(OptimisticLockException.class, RollbackException.class);
		}
		finally {
			if (first.getTransaction().isActive()) first.getTransaction().rollback();
			if (second.getTransaction().isActive()) second.getTransaction().rollback();
			first.close();
			second.close();
		}
	}

	private void assertOptimisticRevocationLock(Long operationId) {
		EntityManager first = entityManagerFactory.createEntityManager();
		EntityManager second = entityManagerFactory.createEntityManager();
		try {
			first.getTransaction().begin();
			second.getTransaction().begin();
			RevocationOperationEntity firstCopy = first.find(RevocationOperationEntity.class, operationId);
			RevocationOperationEntity secondCopy = second.find(RevocationOperationEntity.class, operationId);
			firstCopy.markSubmitted(Instant.now(), "external-first");
			first.getTransaction().commit();
			secondCopy.markSubmitted(Instant.now(), "external-second");
			assertThatThrownBy(second.getTransaction()::commit)
					.isInstanceOfAny(OptimisticLockException.class, RollbackException.class);
		}
		finally {
			if (first.getTransaction().isActive()) first.getTransaction().rollback();
			if (second.getTransaction().isActive()) second.getTransaction().rollback();
			first.close();
			second.close();
		}
	}
}
