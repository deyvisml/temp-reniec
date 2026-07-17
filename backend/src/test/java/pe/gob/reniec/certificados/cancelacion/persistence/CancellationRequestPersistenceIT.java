package pe.gob.reniec.certificados.cancelacion.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RequestLifecycleStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationResult;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "debug=false")
@ActiveProfiles("test")
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
	void cleanDatabaseRunsFlywayValidatesSevenTablesAndReportsSafeHealth() throws Exception {
		Integer tableCount = jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name IN (
				 'certificate_cancellation_request', 'certificate_eligibility_check',
				 'identity_verification', 'cancellation_request_session',
				 'revocation_operation', 'cancellation_receipt', 'cancellation_audit_event')
				""", Integer.class);
		Integer obsoleteCount = jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name IN ('cancellation_process', 'cancellation_session')
				""", Integer.class);
		Integer migrationCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE", Integer.class);
		HttpResponse<String> health = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health")).GET().build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(tableCount).isEqualTo(7);
		assertThat(obsoleteCount).isZero();
		assertThat(migrationCount).isEqualTo(1);
		assertThat(health.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(health.body()).contains("\"status\":\"UP\"")
				.doesNotContain("jdbc", "mysql", "username", "password", "sql");
	}

	@Test
	void storesCurrentRequestReasonConsentRecoveryAndExpirationQueries() {
		Instant now = Instant.now();
		CertificateCancellationRequestEntity request = saveRequest(hex('a'), "1234", now.plus(LIFETIME));
		request.recordEligibility(CurrentEligibilityResult.ELIGIBLE, CancellationRequestStatus.ELIGIBLE);
		request.registerReason(CancellationReasonCode.OTHER, new byte[] { 9, 8, 7 }, "reason-key-v1");
		request.confirm("consent-v1", Instant.now());
		requestRepository.saveAndFlush(request);

		assertThat(requestRepository.findById(request.getId())).get()
				.satisfies(found -> {
					assertThat(found.getRequestStatus()).isEqualTo(CancellationRequestStatus.CONFIRMED);
					assertThat(found.getEligibilityResult()).isEqualTo(CurrentEligibilityResult.ELIGIBLE);
					assertThat(found.getReasonCode()).isEqualTo(CancellationReasonCode.OTHER);
					assertThat(found.getConsentTextVersion()).isEqualTo("consent-v1");
					assertThat(found.getDniCiphertext()).containsExactly(1, 2, 3, 4);
				});
		assertThatThrownBy(() -> request.registerReason(CancellationReasonCode.LOSS, null, null))
				.isInstanceOf(IllegalStateException.class);
		assertThat(requestRepository.findByDniLookupHashAndLifecycleStatus(hex('a'), RequestLifecycleStatus.ACTIVE)).get()
				.extracting(CertificateCancellationRequestEntity::getId).isEqualTo(request.getId());
		assertThat(requestRepository.findFirstByDniLookupHashOrderByCreatedAtDesc(hex('a'))).get()
				.extracting(CertificateCancellationRequestEntity::getId).isEqualTo(request.getId());

		CertificateCancellationRequestEntity expiring = saveRequest(hex('b'), "5678", now.plusSeconds(30));
		assertThat(requestRepository.findByLifecycleStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
				RequestLifecycleStatus.ACTIVE, now.plusSeconds(60))).extracting(CertificateCancellationRequestEntity::getId)
				.contains(expiring.getId());
		expiring.transitionTo(CancellationRequestStatus.ABANDONED, RequestLifecycleStatus.ABANDONED,
				CancellationFinalOutcome.ABANDONED);
		requestRepository.saveAndFlush(expiring);
		assertThat(requestRepository.findByLifecycleStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
				RequestLifecycleStatus.ACTIVE, now.plusSeconds(60))).isEmpty();
	}

	@Test
	void enforcesOneActiveRequestAndOptimisticConcurrencyWhileAllowingHistory() {
		String dniHash = hex('c');
		CertificateCancellationRequestEntity original = saveRequest(dniHash, "1111", Instant.now().plus(LIFETIME));
		assertThatThrownBy(() -> saveRequest(dniHash, "2222", Instant.now().plus(LIFETIME)))
				.isInstanceOf(DataIntegrityViolationException.class);

		original.transitionTo(CancellationRequestStatus.EXPIRED, RequestLifecycleStatus.EXPIRED,
				CancellationFinalOutcome.EXPIRED);
		requestRepository.saveAndFlush(original);
		CertificateCancellationRequestEntity current = saveRequest(dniHash, "3333", Instant.now().plus(LIFETIME));
		assertThat(current.getId()).isNotEqualTo(original.getId());

		EntityManager first = entityManagerFactory.createEntityManager();
		EntityManager second = entityManagerFactory.createEntityManager();
		try {
			first.getTransaction().begin();
			second.getTransaction().begin();
			CertificateCancellationRequestEntity firstCopy = first.find(CertificateCancellationRequestEntity.class, current.getId());
			CertificateCancellationRequestEntity secondCopy = second.find(CertificateCancellationRequestEntity.class, current.getId());
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

	@Test
	void recordsRepeatableEligibilityAndIdentityAttemptsAndFindsLatestValid() {
		CertificateCancellationRequestEntity request = saveRequest(hex('d'), "4444", Instant.now().plus(LIFETIME));
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
				now.plusSeconds(1), "identity-ref-1", null, "REJECTED_BY_PROVIDER");
		identityRepository.saveAndFlush(identity1);
		IdentityVerificationEntity identity2 = new IdentityVerificationEntity(request, 2, "ID_PERU", now.plusSeconds(2), CORRELATION);
		identity2.finish(IdentityVerificationStatus.VERIFIED, IdentityMatchResult.MATCH,
				now.plusSeconds(3), "identity-ref-2", hex('e'), null);
		identityRepository.saveAndFlush(identity2);

		assertThat(eligibilityRepository.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId())).get()
				.extracting(CertificateEligibilityCheckEntity::getId).isEqualTo(eligibility2.getId());
		assertThat(identityRepository.findFirstByRequest_IdAndVerificationStatusOrderByAttemptNumberDesc(
				request.getId(), IdentityVerificationStatus.VERIFIED)).get()
				.extracting(IdentityVerificationEntity::getId).isEqualTo(identity2.getId());
		assertThatThrownBy(() -> eligibilityRepository.saveAndFlush(new CertificateEligibilityCheckEntity(
				request, 2, EligibilityCheckStatus.CREATED, now.plusSeconds(4), CORRELATION)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void storesMultipleSessionsAndReturnsOnlyActiveOnes() {
		CertificateCancellationRequestEntity request = saveRequest(hex('f'), "5555", Instant.now().plus(LIFETIME));
		Instant now = Instant.now();
		CancellationRequestSessionEntity active = sessionRepository.saveAndFlush(
				new CancellationRequestSessionEntity(request, hex('1'), UUID.randomUUID(), now.plusSeconds(120), hex('2')));
		CancellationRequestSessionEntity invalidated = sessionRepository.saveAndFlush(
				new CancellationRequestSessionEntity(request, hex('3'), UUID.randomUUID(), now.plusSeconds(120), null));
		invalidated.invalidate(Instant.now(), "RECOVERED_ON_ANOTHER_DEVICE");
		sessionRepository.saveAndFlush(invalidated);
		CancellationRequestSessionEntity expired = sessionRepository.saveAndFlush(
				new CancellationRequestSessionEntity(request, hex('4'), UUID.randomUUID(), now.plusMillis(500), null));

		List<CancellationRequestSessionEntity> sessions = sessionRepository
				.findByRequest_IdAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
						request.getId(), now.plusSeconds(1));
		assertThat(sessions).extracting(CancellationRequestSessionEntity::getId)
				.containsExactly(active.getId()).doesNotContain(invalidated.getId(), expired.getId());
	}

	@Test
	void enforcesRevocationIdempotencyAttemptAndOpenOperationGuards() {
		CertificateCancellationRequestEntity request = saveRequest(hex('5'), "6666", Instant.now().plus(LIFETIME));
		UUID key = UUID.randomUUID();
		Instant now = Instant.now();
		RevocationOperationEntity uncertain = new RevocationOperationEntity(request, key, 1, now, CORRELATION);
		uncertain.markSubmitted(now.plusSeconds(1), "revocation-ref-1");
		uncertain.complete(RevocationOperationStatus.OUTCOME_UNKNOWN, RevocationResult.OUTCOME_UNKNOWN,
				now.plusSeconds(2), null, "PROVIDER_TIMEOUT", now.plusSeconds(60));
		revocationRepository.saveAndFlush(uncertain);

		assertThat(revocationRepository.findByIdempotencyKey(key)).get()
				.extracting(RevocationOperationEntity::getId).isEqualTo(uncertain.getId());
		assertThat(revocationRepository.findFirstByRequest_IdAndOperationStatusInOrderByAttemptNumberDesc(
				request.getId(), List.of(RevocationOperationStatus.PREPARED, RevocationOperationStatus.SUBMITTED,
						RevocationOperationStatus.OUTCOME_UNKNOWN))).get()
				.extracting(RevocationOperationEntity::getId).isEqualTo(uncertain.getId());
		assertThatThrownBy(() -> revocationRepository.saveAndFlush(
				new RevocationOperationEntity(request, UUID.randomUUID(), 2, now.plusSeconds(3), CORRELATION)))
				.isInstanceOf(DataIntegrityViolationException.class);

		uncertain.complete(RevocationOperationStatus.FAILED, RevocationResult.FAILED,
				now.plusSeconds(4), now.plusSeconds(4), "CONFIRMED_FAILURE", null);
		revocationRepository.saveAndFlush(uncertain);
		RevocationOperationEntity retry = revocationRepository.saveAndFlush(
				new RevocationOperationEntity(request, UUID.randomUUID(), 2, now.plusSeconds(5), CORRELATION));
		assertThat(retry.getAttemptNumber()).isEqualTo(2);
		assertThatThrownBy(() -> revocationRepository.saveAndFlush(
				new RevocationOperationEntity(request, key, 3, now.plusSeconds(6), CORRELATION)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsConcurrentRevocationOverwrite() {
		CertificateCancellationRequestEntity request = saveRequest(hex('9'), "9999", Instant.now().plus(LIFETIME));
		RevocationOperationEntity operation = revocationRepository.saveAndFlush(new RevocationOperationEntity(
				request, UUID.randomUUID(), 1, Instant.now(), CORRELATION));
		EntityManager first = entityManagerFactory.createEntityManager();
		EntityManager second = entityManagerFactory.createEntityManager();
		try {
			first.getTransaction().begin();
			second.getTransaction().begin();
			RevocationOperationEntity firstCopy = first.find(RevocationOperationEntity.class, operation.getId());
			RevocationOperationEntity secondCopy = second.find(RevocationOperationEntity.class, operation.getId());
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

	@Test
	void associatesReceiptsOnlyWithSuccessfulRevocationAndKeepsReceiptFailureIndependent() {
		CertificateCancellationRequestEntity request = saveRequest(hex('6'), "7777", Instant.now().plus(LIFETIME));
		Instant now = Instant.now();
		RevocationOperationEntity operation = new RevocationOperationEntity(request, UUID.randomUUID(), 1, now, CORRELATION);
		operation.complete(RevocationOperationStatus.SUCCEEDED, RevocationResult.SUCCEEDED,
				now.plusSeconds(1), now.plusSeconds(1), null, null);
		revocationRepository.saveAndFlush(operation);
		request.transitionTo(CancellationRequestStatus.COMPLETED, RequestLifecycleStatus.FINALIZED,
				CancellationFinalOutcome.REVOCATION_SUCCEEDED);
		requestRepository.saveAndFlush(request);

		CancellationReceiptEntity available = new CancellationReceiptEntity(request, operation, "CD-TEST-0001", "template-v1");
		available.markAvailable("documents/receipt-0001", hex('7'), now.plusSeconds(2), now.plusSeconds(3));
		receiptRepository.saveAndFlush(available);
		CancellationReceiptEntity failed = new CancellationReceiptEntity(request, operation, "CD-TEST-0002", "template-v1");
		failed.markFailed("RENDER_FAILURE");
		receiptRepository.saveAndFlush(failed);

		assertThat(receiptRepository.findFirstByRequest_IdAndGenerationStatusOrderByAvailableAtDesc(
				request.getId(), ReceiptGenerationStatus.AVAILABLE)).get()
				.extracting(CancellationReceiptEntity::getId).isEqualTo(available.getId());
		assertThat(requestRepository.findById(request.getId())).get()
				.extracting(CertificateCancellationRequestEntity::getFinalOutcome)
				.isEqualTo(CancellationFinalOutcome.REVOCATION_SUCCEEDED);
		assertThatThrownBy(() -> receiptRepository.saveAndFlush(
				new CancellationReceiptEntity(request, operation, "CD-TEST-0001", "template-v2")))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name = 'cancellation_receipt'
				  AND data_type IN ('blob', 'mediumblob', 'longblob')
				""", Integer.class)).isZero();
	}

	@Test
	void appendsOrderedAuditHistoryAndRejectsOrphanRows() {
		CertificateCancellationRequestEntity request = saveRequest(hex('8'), "8888", Instant.now().plus(LIFETIME));
		Instant now = Instant.now();
		CancellationAuditEventEntity started = auditRepository.save(new CancellationAuditEventEntity(
				request, CancellationAuditEventType.REQUEST_STARTED, null, CancellationRequestStatus.STARTED,
				"CREATED", CORRELATION, null, null, null, AuditEventOrigin.CITIZEN, now));
		CancellationAuditEventEntity checked = auditRepository.save(new CancellationAuditEventEntity(
				request, CancellationAuditEventType.ELIGIBILITY_CHECKED, CancellationRequestStatus.STARTED,
				CancellationRequestStatus.ELIGIBLE, "ELIGIBLE", CORRELATION, "eligibility-ref", null,
				null, AuditEventOrigin.EXTERNAL_PROVIDER, now.plusSeconds(1)));

		assertThat(auditRepository.findByRequest_IdOrderByOccurredAtAscIdAsc(request.getId()))
				.extracting(CancellationAuditEventEntity::getId).containsExactly(started.getId(), checked.getId());
		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO cancellation_audit_event
				(id, request_id, event_type, correlation_id, event_origin, occurred_at)
				VALUES (?, ?, 'REQUEST_STARTED', ?, 'SYSTEM', CURRENT_TIMESTAMP(6))
				""", uuidBytes(UUID.randomUUID()), uuidBytes(UUID.randomUUID()), CORRELATION))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private CertificateCancellationRequestEntity saveRequest(String hash, String lastFour, Instant expiry) {
		return requestRepository.saveAndFlush(new CertificateCancellationRequestEntity(
				hash, new byte[] { 1, 2, 3, 4 }, "dni-key-v1", lastFour,
				Instant.now().plus(Duration.ofHours(2)), expiry));
	}

	private static String hex(char character) { return String.valueOf(character).repeat(64); }

	private static byte[] uuidBytes(UUID uuid) {
		return ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
	}
}
