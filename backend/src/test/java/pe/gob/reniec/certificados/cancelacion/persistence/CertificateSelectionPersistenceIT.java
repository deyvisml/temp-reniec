package pe.gob.reniec.certificados.cancelacion.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import pe.gob.reniec.certificados.cancelacion.cancellation.eligibility.CancellationRequestResponse;
import pe.gob.reniec.certificados.cancelacion.cancellation.eligibility.EligibilityInitiationService;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationFinalOutcome;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReasonCode;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReceiptEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReceiptRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestCertificateEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestCertificateRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateAvailabilityStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationResult;

@Testcontainers
@SpringBootTest(properties = "debug=false")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CertificateSelectionPersistenceIT extends MySqlContainerSupport {

	private static final Instant NOW = Instant.parse("2026-07-20T15:00:00Z");

	@Autowired CertificateCancellationRequestRepository requestRepository;
	@Autowired CancellationRequestCertificateRepository certificateRepository;
	@Autowired RevocationOperationRepository operationRepository;
	@Autowired CancellationReceiptRepository receiptRepository;
	@Autowired EligibilityInitiationService eligibilityInitiationService;
	@Autowired EntityManagerFactory entityManagerFactory;
	@Autowired JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanTables() {
		jdbcTemplate.update("DELETE FROM cancellation_audit_event");
		jdbcTemplate.update("DELETE FROM cancellation_receipt");
		jdbcTemplate.update("DELETE FROM cancellation_request_certificate");
		jdbcTemplate.update("DELETE FROM revocation_operation");
		jdbcTemplate.update("DELETE FROM identity_verification");
		jdbcTemplate.update("DELETE FROM certificate_availability_check");
		jdbcTemplate.update("DELETE FROM certificate_cancellation_request");
	}

	@Test
	@Transactional
	void supportsNoOneSeveralOrAllCertificateSelections() {
		RequestFixture empty = request("10000001", 1);
		assertThat(certificateRepository.countByRequest_Id(empty.request().getId())).isZero();

		RequestFixture one = request("10000002", 1);
		CancellationRequestCertificateEntity only = certificate(one, "ORD-1",
				"0e8c9f44-04da-4b74-94b5-a1caec20f1e1", 1);
		only.select(NOW.plusSeconds(1));
		certificateRepository.saveAndFlush(only);
		assertThat(certificateRepository.countByRequest_IdAndSelectedTrue(one.request().getId())).isEqualTo(1);

		RequestFixture many = request("10000003", 1);
		CancellationRequestCertificateEntity first = certificate(many, "ORD-2",
				"3ff0c799-5845-4c30-bb3d-f5ea260dad61", 2);
		CancellationRequestCertificateEntity second = certificate(many, "ORD-3",
				"31ab4d38-e7ef-47af-af8c-f7fedc05a1d2", 3);
		CancellationRequestCertificateEntity third = certificate(many, "ORD-4",
				"4434db81-7563-4614-adbb-f69072548b4b", 4);
		first.select(NOW.plusSeconds(2));
		third.select(NOW.plusSeconds(3));
		certificateRepository.saveAllAndFlush(List.of(first, second, third));
		assertThat(certificateRepository.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(
				many.request().getId())).extracting(CancellationRequestCertificateEntity::getOrderNumber)
				.containsExactly("ORD-2", "ORD-4");

		CancellationRequestCertificateEntity secondReloaded = certificateRepository.findById(second.getId()).orElseThrow();
		secondReloaded.select(Instant.now().plusSeconds(4));
		certificateRepository.saveAndFlush(secondReloaded);
		assertThat(certificateRepository.countByRequest_IdAndSelectedTrue(many.request().getId())).isEqualTo(3);
	}

	@Test
	void enforcesUuidRequestOwnershipAndSelectionIntegrity() {
		RequestFixture first = request("20000001", 1);
		RequestFixture second = request("20000002", 1);
		String sharedUuid = "0dcde0fc-5e1f-4f28-b9be-52aafaa10240";
		certificate(first, "ORD-A", sharedUuid, 1);

		assertThatThrownBy(() -> certificate(first, "ORD-B", sharedUuid, 2))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(certificate(second, "ORD-C", sharedUuid, 3).getId()).isNotNull();
		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO cancellation_request_certificate
				(request_id, order_number, emission_created_at, certificate_uuid,
				 availability_status, consulted_at, selected, selected_at, version, created_at, updated_at)
				VALUES (?, 'ORPHAN', ?, '68d769c6-a58f-4dbd-b668-80a3a36c0524',
				 'AVAILABLE', ?, FALSE, NULL, 0, ?, ?)
				""", Long.MAX_VALUE, NOW.minusSeconds(30), NOW, NOW, NOW))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbcTemplate.update("""
				UPDATE cancellation_request_certificate SET selected = TRUE, selected_at = NULL
				WHERE request_id = ?
				""", first.request().getId())).isInstanceOf(DataAccessException.class);
	}

	@Test
	@Transactional
	void makesConfirmedSelectionImmutableAndRejectsNewRows() {
		RequestFixture fixture = request("30000001", 1);
		CancellationRequestCertificateEntity selected = certificate(fixture, "ORD-1",
				"bafdbbb4-33fe-438f-992c-00f664770e9a", 1);
		selected.select(NOW.plusSeconds(2));
		certificateRepository.saveAndFlush(selected);
		confirm(fixture.request());

		assertThatThrownBy(selected::deselect).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("confirmed");
		assertThatThrownBy(() -> certificate(fixture, "ORD-2",
				"ee4ed984-00de-4490-8521-02426ad0d69c", 3))
				.hasRootCauseInstanceOf(IllegalStateException.class).hasMessageContaining("confirmation");
		assertThatThrownBy(() -> {
			certificateRepository.delete(selected);
			certificateRepository.flush();
		}).hasRootCauseInstanceOf(IllegalStateException.class).hasMessageContaining("confirmed");
	}

	@Test
	@Transactional
	void appliesSuccessfulFailedAndUnknownOutcomesToTheWholeSelectedSet() {
		assertAtomicOutcome("40000001", RevocationOperationStatus.SUCCEEDED, RevocationResult.SUCCEEDED,
				CertificateAvailabilityStatus.REVOKED, CancellationRequestStatus.REVOCATION_SUCCEEDED,
				CancellationFinalOutcome.REVOCATION_SUCCEEDED);
		assertAtomicOutcome("40000002", RevocationOperationStatus.FAILED, RevocationResult.FAILED,
				CertificateAvailabilityStatus.REVOCATION_FAILED, CancellationRequestStatus.REVOCATION_FAILED,
				CancellationFinalOutcome.REVOCATION_FAILED);
		assertAtomicOutcome("40000003", RevocationOperationStatus.OUTCOME_UNKNOWN, RevocationResult.OUTCOME_UNKNOWN,
				CertificateAvailabilityStatus.OUTCOME_UNKNOWN, CancellationRequestStatus.REVOCATION_OUTCOME_UNKNOWN,
				CancellationFinalOutcome.OUTCOME_UNKNOWN);
	}

	@Test
	void rejectsMixedNormalizationAndPreservesUncertainIdempotency() {
		RequestFixture fixture = request("50000001", 1);
		RevocationOperationEntity operation = new RevocationOperationEntity(
				fixture.request(), "idem-unknown-1", 1, NOW, "corr-unknown");
		operation.markSubmitted(NOW.plusSeconds(1), "external-unknown");
		assertThatThrownBy(() -> operation.complete(RevocationOperationStatus.SUCCEEDED,
				RevocationResult.FAILED, NOW.plusSeconds(2), NOW.plusSeconds(2), null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must match");
		operation.complete(RevocationOperationStatus.OUTCOME_UNKNOWN, RevocationResult.OUTCOME_UNKNOWN,
				NOW.plusSeconds(2), NOW.plusSeconds(2), "UNCONFIRMED");
		operationRepository.saveAndFlush(operation);

		assertThat(operationRepository.findById(operation.getId())).get()
				.extracting(RevocationOperationEntity::getIdempotencyKey, RevocationOperationEntity::getNormalizedResult)
				.containsExactly("idem-unknown-1", RevocationResult.OUTCOME_UNKNOWN);
		assertThatThrownBy(() -> operationRepository.saveAndFlush(new RevocationOperationEntity(
				fixture.request(), "idem-unknown-1", 2, NOW.plusSeconds(3), "corr-replacement")))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(operationRepository.count()).isEqualTo(1);
	}

	@Test
	void detectsConcurrentSelectionUpdatesBeforeConfirmation() {
		RequestFixture fixture = request("60000001", 1);
		CancellationRequestCertificateEntity certificate = certificate(fixture, "ORD-1",
				"1bc93fb8-ed89-4acd-ae0c-3eca1328a7d0", 1);
		EntityManager firstManager = entityManagerFactory.createEntityManager();
		EntityManager secondManager = entityManagerFactory.createEntityManager();
		try {
			firstManager.getTransaction().begin();
			secondManager.getTransaction().begin();
			CancellationRequestCertificateEntity firstCopy = firstManager.find(
					CancellationRequestCertificateEntity.class, certificate.getId());
			CancellationRequestCertificateEntity secondCopy = secondManager.find(
					CancellationRequestCertificateEntity.class, certificate.getId());
			firstCopy.select(NOW.plusSeconds(10));
			firstManager.getTransaction().commit();
			secondCopy.select(NOW.plusSeconds(11));
			assertThatThrownBy(() -> secondManager.getTransaction().commit())
					.isInstanceOfAny(RollbackException.class, OptimisticLockException.class);
		}
		finally {
			if (firstManager.getTransaction().isActive()) firstManager.getTransaction().rollback();
			if (secondManager.getTransaction().isActive()) secondManager.getTransaction().rollback();
			firstManager.close();
			secondManager.close();
		}
	}

	@Test
	@Transactional
	void startsFreshConsultationAfterCompletedAtomicCancellationAndPreservesHistory() {
		RequestFixture historical = request("00000001", 1);
		CancellationRequestCertificateEntity selected = certificate(historical, "ORD-HIST-1",
				"4d4aa8cc-3263-4c65-960f-a1823cc708af", 1);
		selected.select(NOW.plusSeconds(3));
		certificateRepository.saveAndFlush(selected);
		confirm(historical.request());
		RevocationOperationEntity operation = new RevocationOperationEntity(
				historical.request(), "idem-history-1", 1, NOW.plusSeconds(4), "corr-history");
		operation.markSubmitted(NOW.plusSeconds(5), "external-history");
		operation.complete(RevocationOperationStatus.SUCCEEDED, RevocationResult.SUCCEEDED,
				NOW.plusSeconds(6), NOW.plusSeconds(6), null);
		operationRepository.saveAndFlush(operation);
		selected.applyAtomicOutcome(RevocationResult.SUCCEEDED);
		certificateRepository.saveAndFlush(selected);
		CancellationReceiptEntity receipt = new CancellationReceiptEntity(
				historical.request(), operation, "CD-HISTORY-001");
		receipt.markAvailable("receipts/history-001.pdf", NOW.plusSeconds(7), NOW.plusSeconds(8));
		receiptRepository.saveAndFlush(receipt);
		historical.request().transitionTo(CancellationRequestStatus.RECEIPT_AVAILABLE,
				CancellationFinalOutcome.REVOCATION_SUCCEEDED);
		requestRepository.saveAndFlush(historical.request());

		CancellationRequestResponse fresh = eligibilityInitiationService.initiate("00000001", "corr-fresh");

		assertThat(fresh.requestId()).isNotEqualTo(historical.request().getId());
		assertThat(requestRepository.findById(historical.request().getId())).get()
				.extracting(CertificateCancellationRequestEntity::getRequestStatus)
				.isEqualTo(CancellationRequestStatus.RECEIPT_AVAILABLE);
		assertThat(certificateRepository.findById(selected.getId())).isPresent();
		assertThat(operationRepository.findById(operation.getId())).isPresent();
		assertThat(receiptRepository.findById(receipt.getId())).isPresent();
	}

	private void assertAtomicOutcome(String dni, RevocationOperationStatus operationStatus, RevocationResult result,
			CertificateAvailabilityStatus certificateStatus, CancellationRequestStatus requestStatus,
			CancellationFinalOutcome finalOutcome) {
		RequestFixture fixture = request(dni, 1);
		CancellationRequestCertificateEntity first = certificate(fixture, "ORD-1", uuidFor(dni, 1), 1);
		CancellationRequestCertificateEntity second = certificate(fixture, "ORD-2", uuidFor(dni, 2), 2);
		CancellationRequestCertificateEntity unselected = certificate(fixture, "ORD-3", uuidFor(dni, 3), 3);
		first.select(NOW.plusSeconds(4));
		second.select(NOW.plusSeconds(4));
		certificateRepository.saveAllAndFlush(List.of(first, second));
		confirm(fixture.request());
		RevocationOperationEntity operation = new RevocationOperationEntity(
				fixture.request(), "idem-" + dni, 1, NOW.plusSeconds(5), "corr-" + dni);
		operation.markSubmitted(NOW.plusSeconds(6), "external-" + dni);
		operation.complete(operationStatus, result, NOW.plusSeconds(7), NOW.plusSeconds(7),
				result == RevocationResult.FAILED ? "REJECTED" : null);
		operationRepository.saveAndFlush(operation);
		first.applyAtomicOutcome(result);
		second.applyAtomicOutcome(result);
		certificateRepository.saveAllAndFlush(List.of(first, second));
		fixture.request().transitionTo(requestStatus, finalOutcome);
		requestRepository.saveAndFlush(fixture.request());

		assertThat(certificateRepository.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(
				fixture.request().getId())).extracting(CancellationRequestCertificateEntity::getAvailabilityStatus)
				.containsOnly(certificateStatus);
		assertThat(certificateRepository.findById(unselected.getId())).get()
				.extracting(CancellationRequestCertificateEntity::getAvailabilityStatus)
				.isEqualTo(CertificateAvailabilityStatus.AVAILABLE);
		assertThat(operationRepository.findById(operation.getId())).get()
				.extracting(RevocationOperationEntity::getNormalizedResult).isEqualTo(result);
	}

	private void confirm(CertificateCancellationRequestEntity request) {
		request.registerReason(CancellationReasonCode.THEFT, null);
		request.confirm(Instant.now().plusSeconds(1));
		requestRepository.saveAndFlush(request);
	}

	private RequestFixture request(String dni, int attempt) {
		CertificateCancellationRequestEntity request = requestRepository.saveAndFlush(
				new CertificateCancellationRequestEntity(dni));
		return new RequestFixture(request);
	}

	private CancellationRequestCertificateEntity certificate(RequestFixture fixture, String order,
			String uuid, long seconds) {
		CancellationRequestCertificateEntity certificate = new CancellationRequestCertificateEntity(
				fixture.request(), order, NOW.minus(1, ChronoUnit.DAYS), uuid,
				NOW.plusSeconds(seconds));
		return certificateRepository.saveAndFlush(certificate);
	}

	private String uuidFor(String dni, int suffix) {
		return "00000000-0000-4000-8000-" + dni + String.format("%04d", suffix);
	}

	private record RequestFixture(CertificateCancellationRequestEntity request) { }
}
