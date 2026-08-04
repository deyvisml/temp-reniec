package pe.gob.reniec.credenciales.revocacion.persistence;

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

import pe.gob.reniec.credenciales.revocacion.revocation.initiation.RevocationRequestResponse;
import pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialListResponse;
import pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialListingException;
import pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialListingService;
import pe.gob.reniec.credenciales.revocacion.revocation.initiation.RevocationRequestInitiationService;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationFinalOutcome;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReasonCode;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReceiptEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReceiptRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestDigitalCredentialEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestDigitalCredentialRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialAvailabilityStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationResult;
import pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationConfirmationRequest;
import pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationReviewRequest;
import pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationConfirmationService;
import pe.gob.reniec.credenciales.revocacion.revocation.confirmation.RevocationConsentCatalog;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationAuditEventRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationAuditEventType;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.CurrentAvailabilityResult;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityMatchResult;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationStatus;

@Testcontainers
@SpringBootTest(properties = "debug=false")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DigitalCredentialSelectionPersistenceIT extends MySqlContainerSupport {

	private static final Instant NOW = Instant.parse("2026-07-20T15:00:00Z");

	@Autowired DigitalCredentialRevocationRequestRepository requestRepository;
	@Autowired RevocationRequestDigitalCredentialRepository digitalCredentialRepository;
	@Autowired RevocationOperationRepository operationRepository;
	@Autowired RevocationReceiptRepository receiptRepository;
	@Autowired RevocationRequestInitiationService revocationRequestInitiationService;
	@Autowired DigitalCredentialListingService digitalCredentialListingService;
	@Autowired EntityManagerFactory entityManagerFactory;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired RevocationConfirmationService confirmationService;
	@Autowired IdentityVerificationRepository identityRepository;
	@Autowired RevocationAuditEventRepository auditRepository;

	@BeforeEach
	void cleanTables() {
		jdbcTemplate.update("DELETE FROM revocation_audit_event");
		jdbcTemplate.update("DELETE FROM revocation_receipt");
		jdbcTemplate.update("DELETE FROM revocation_request_digital_credential");
		jdbcTemplate.update("DELETE FROM revocation_operation");
		jdbcTemplate.update("DELETE FROM identity_verification");
		jdbcTemplate.update("DELETE FROM digital_credential_availability_check");
		jdbcTemplate.update("DELETE FROM digital_credential_revocation_request");
	}

	@Test
	void listsPersistsAndConfirmsAnExactDigitalCredentialSelection() {
		DigitalCredentialRevocationRequestEntity request = identityVerifiedRequest("00000022");

		DigitalCredentialListResponse listed = digitalCredentialListingService.list(request.getId(), "list-correlation");

		assertThat(listed.requestStatus()).isEqualTo("DIGITAL_CREDENTIALS_AVAILABLE");
		assertThat(listed.digitalCredentials()).hasSize(3);
		assertThat(digitalCredentialRepository.countByRequest_Id(request.getId())).isEqualTo(3);
		List<Long> originalIds = digitalCredentialRepository
				.findByRequest_IdOrderByEmissionCreatedAtAscIdAsc(request.getId()).stream()
				.map(RevocationRequestDigitalCredentialEntity::getId).toList();

		listed = digitalCredentialListingService.list(request.getId(), "refresh-correlation");

		assertThat(listed.digitalCredentials()).hasSize(3);
		assertThat(digitalCredentialRepository.countByRequest_Id(request.getId())).isEqualTo(3);
		assertThat(digitalCredentialRepository.findByRequest_IdOrderByEmissionCreatedAtAscIdAsc(request.getId()))
				.extracting(RevocationRequestDigitalCredentialEntity::getId)
				.doesNotContainAnyElementsOf(originalIds);
		String selectedUuid = listed.digitalCredentials().get(1).digitalCredentialUuid();
		int selectedIndex = listed.digitalCredentials().get(1).statusListIndex();

		confirmationService.preview(request.getId(),
				new RevocationReviewRequest(selectedUuid, selectedIndex, RevocationReasonCode.LOSS, null));
		assertThat(digitalCredentialRepository.countByRequest_IdAndSelectedTrue(request.getId())).isZero();
		assertThat(requestRepository.findById(request.getId())).get()
				.extracting(DigitalCredentialRevocationRequestEntity::getReasonCode).isNull();

		confirmationService.confirm(request.getId(),
				new RevocationConfirmationRequest(selectedUuid, selectedIndex, RevocationReasonCode.LOSS, null,
						true, RevocationConsentCatalog.VERSION),
				"confirmation-correlation");
		assertThat(digitalCredentialRepository.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(
				request.getId())).extracting(RevocationRequestDigitalCredentialEntity::getDigitalCredentialUuid)
				.containsExactly(selectedUuid);
		assertThat(requestRepository.findById(request.getId())).get()
				.extracting(DigitalCredentialRevocationRequestEntity::getRequestStatus)
				.isEqualTo(RevocationRequestStatus.CONFIRMED);
	}

	@Test
	void persistsNoCredentialsWhenTheOfficialListIsEmpty() {
		DigitalCredentialRevocationRequestEntity request = identityVerifiedRequest("00000020");

		DigitalCredentialListResponse response = digitalCredentialListingService.list(
				request.getId(), "empty-correlation");
		assertThat(response.requestStatus()).isEqualTo("NO_DIGITAL_CREDENTIALS_AVAILABLE");
		assertThat(response.digitalCredentials()).isEmpty();
		assertThat(digitalCredentialRepository.countByRequest_Id(request.getId())).isZero();
		assertThat(requestRepository.findById(request.getId())).get()
				.extracting(DigitalCredentialRevocationRequestEntity::getRequestStatus)
				.isEqualTo(RevocationRequestStatus.NO_DIGITAL_CREDENTIALS_AVAILABLE);
	}

	@Test
	void persistsRepeatedUuidsAndConfirmsTheRequestedIndex() {
		DigitalCredentialRevocationRequestEntity request = identityVerifiedRequest("00000022");
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE);
		request = requestRepository.saveAndFlush(request);
		String repeatedUuid = "e87a7813-880d-4a2d-92f7-4251c841d008";
		Instant consultedAt = NOW.minusSeconds(5);
		RevocationRequestDigitalCredentialEntity index11 = new RevocationRequestDigitalCredentialEntity(
				request, 11, "DniPeruanoCredential", NOW.minusSeconds(120), repeatedUuid,
				DigitalCredentialAvailabilityStatus.AVAILABLE, null, 0, consultedAt);
		RevocationRequestDigitalCredentialEntity index12 = new RevocationRequestDigitalCredentialEntity(
				request, 12, "DniPeruanoCredential", NOW.minusSeconds(60), repeatedUuid,
				DigitalCredentialAvailabilityStatus.AVAILABLE, null, 0, consultedAt);
		digitalCredentialRepository.saveAllAndFlush(List.of(index11, index12));

		confirmationService.confirm(request.getId(),
				new RevocationConfirmationRequest(repeatedUuid, 12, RevocationReasonCode.LOSS, null,
						true, RevocationConsentCatalog.VERSION),
				"repeated-uuid-confirmation");

		assertThat(digitalCredentialRepository.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(
				request.getId()))
				.singleElement()
				.extracting(RevocationRequestDigitalCredentialEntity::getStatusListIndex)
				.isEqualTo(12);
	}

	@Test
	void rejectsInvalidProviderDataAndRestoresTheRetryableState() {
		DigitalCredentialRevocationRequestEntity request = identityVerifiedRequest("00000029");

		assertThatThrownBy(() -> digitalCredentialListingService.list(request.getId(), "invalid-correlation"))
				.isInstanceOf(DigitalCredentialListingException.class)
				.extracting(error -> ((DigitalCredentialListingException) error).reason())
				.isEqualTo(DigitalCredentialListingException.Reason.INVALID_PROVIDER_RESPONSE);
		assertThat(digitalCredentialRepository.countByRequest_Id(request.getId())).isZero();
		assertThat(requestRepository.findById(request.getId())).get()
				.extracting(DigitalCredentialRevocationRequestEntity::getRequestStatus)
				.isEqualTo(RevocationRequestStatus.AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST);
	}

	@Test
	@Transactional
	void uniqueIndexAllowsZeroOrOneSelectionAndRejectsASecondSelection() {
		RequestFixture empty = request("10000001", 1);
		assertThat(digitalCredentialRepository.countByRequest_Id(empty.request().getId())).isZero();

		RequestFixture one = request("10000002", 1);
		RevocationRequestDigitalCredentialEntity only = digitalCredential(one, "ORD-1",
				"0e8c9f44-04da-4b74-94b5-a1caec20f1e1", 1);
		only.select(NOW.plusSeconds(1));
		digitalCredentialRepository.saveAndFlush(only);
		assertThat(digitalCredentialRepository.countByRequest_IdAndSelectedTrue(one.request().getId())).isEqualTo(1);

		RequestFixture many = request("10000003", 1);
		RevocationRequestDigitalCredentialEntity first = digitalCredential(many, "ORD-2",
				"3ff0c799-5845-4c30-bb3d-f5ea260dad61", 2);
		RevocationRequestDigitalCredentialEntity second = digitalCredential(many, "ORD-3",
				"31ab4d38-e7ef-47af-af8c-f7fedc05a1d2", 3);
		RevocationRequestDigitalCredentialEntity third = digitalCredential(many, "ORD-4",
				"4434db81-7563-4614-adbb-f69072548b4b", 4);
		first.select(NOW.plusSeconds(2));
		digitalCredentialRepository.saveAllAndFlush(List.of(first, second, third));
		assertThat(digitalCredentialRepository.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(
				many.request().getId())).extracting(RevocationRequestDigitalCredentialEntity::getStatusListIndex)
				.containsExactly(2);

		RevocationRequestDigitalCredentialEntity secondReloaded = digitalCredentialRepository.findById(second.getId()).orElseThrow();
		secondReloaded.select(Instant.now().plusSeconds(4));
		assertThatThrownBy(() -> digitalCredentialRepository.saveAndFlush(secondReloaded))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void enforcesTupleOwnershipIndexUniquenessAndSelectionIntegrity() {
		RequestFixture first = request("20000001", 1);
		RequestFixture second = request("20000002", 1);
		String sharedUuid = "0dcde0fc-5e1f-4f28-b9be-52aafaa10240";
		digitalCredential(first, "ORD-A", sharedUuid, 1);

		assertThat(digitalCredential(first, "ORD-B", sharedUuid, 2).getId()).isNotNull();
		assertThatThrownBy(() -> digitalCredential(first, "ORD-INDEX-DUPLICATE",
				"7f315ed2-ef17-4af1-865f-0a7784df7d77", 1))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(digitalCredential(second, "ORD-C", sharedUuid, 3).getId()).isNotNull();
		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO revocation_request_digital_credential
				(request_id, legacy_order_number, status_list_index, credential_type,
				 provider_credential_status, emission_created_at, digital_credential_uuid,
				 availability_status, consulted_at, selected, selected_at, version, created_at, updated_at)
				VALUES (?, NULL, 99, 'DniPeruanoCredential', 0, ?,
				 '68d769c6-a58f-4dbd-b668-80a3a36c0524', 'AVAILABLE', ?, FALSE, NULL, 0, ?, ?)
				""", Long.MAX_VALUE, NOW.minusSeconds(30), NOW, NOW, NOW))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbcTemplate.update("""
				UPDATE revocation_request_digital_credential SET selected = TRUE, selected_at = NULL
				WHERE request_id = ?
				""", first.request().getId())).isInstanceOf(DataAccessException.class);
	}

	@Test
	@Transactional
	void makesConfirmedSelectionImmutableAndRejectsNewRows() {
		RequestFixture fixture = request("30000001", 1);
		RevocationRequestDigitalCredentialEntity selected = digitalCredential(fixture, "ORD-1",
				"bafdbbb4-33fe-438f-992c-00f664770e9a", 1);
		selected.select(NOW.plusSeconds(2));
		digitalCredentialRepository.saveAndFlush(selected);
		confirm(fixture.request());

		assertThatThrownBy(selected::deselect).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("confirmed");
		assertThatThrownBy(() -> digitalCredential(fixture, "ORD-2",
				"ee4ed984-00de-4490-8521-02426ad0d69c", 3))
				.hasRootCauseInstanceOf(IllegalStateException.class).hasMessageContaining("confirmation");
		assertThatThrownBy(() -> {
			digitalCredentialRepository.delete(selected);
			digitalCredentialRepository.flush();
		}).hasRootCauseInstanceOf(IllegalStateException.class).hasMessageContaining("confirmed");
	}

	@Test
	@Transactional
	void appliesSuccessfulFailedAndUnknownOutcomesToTheSelectedDigitalCredential() {
		assertAtomicOutcome("40000001", RevocationOperationStatus.SUCCEEDED, RevocationResult.SUCCEEDED,
				DigitalCredentialAvailabilityStatus.REVOKED, RevocationRequestStatus.REVOCATION_SUCCEEDED,
				RevocationFinalOutcome.REVOCATION_SUCCEEDED);
		assertAtomicOutcome("40000002", RevocationOperationStatus.FAILED, RevocationResult.FAILED,
				DigitalCredentialAvailabilityStatus.REVOCATION_FAILED, RevocationRequestStatus.REVOCATION_FAILED,
				RevocationFinalOutcome.REVOCATION_FAILED);
		assertAtomicOutcome("40000003", RevocationOperationStatus.OUTCOME_UNKNOWN, RevocationResult.OUTCOME_UNKNOWN,
				DigitalCredentialAvailabilityStatus.OUTCOME_UNKNOWN, RevocationRequestStatus.REVOCATION_OUTCOME_UNKNOWN,
				RevocationFinalOutcome.OUTCOME_UNKNOWN);
	}

	@Test
	void rejectsMixedNormalizationAndPreservesUncertainIdempotency() {
		RequestFixture fixture = request("50000001", 1);
		RevocationOperationEntity operation = new RevocationOperationEntity(
				fixture.request(), "idem-unknown-1", 1, NOW, "corr-unknown");
		operation.markSubmitted(NOW.plusSeconds(1), "external-unknown");
		assertThatThrownBy(() -> operation.complete(RevocationOperationStatus.SUCCEEDED,
				RevocationResult.FAILED, NOW.plusSeconds(2), NOW.plusSeconds(2), null, null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must match");
		operation.complete(RevocationOperationStatus.OUTCOME_UNKNOWN, RevocationResult.OUTCOME_UNKNOWN,
				NOW.plusSeconds(2), NOW.plusSeconds(2), null, "UNCONFIRMED");
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
		RevocationRequestDigitalCredentialEntity digitalCredential = digitalCredential(fixture, "ORD-1",
				"1bc93fb8-ed89-4acd-ae0c-3eca1328a7d0", 1);
		EntityManager firstManager = entityManagerFactory.createEntityManager();
		EntityManager secondManager = entityManagerFactory.createEntityManager();
		try {
			firstManager.getTransaction().begin();
			secondManager.getTransaction().begin();
			RevocationRequestDigitalCredentialEntity firstCopy = firstManager.find(
					RevocationRequestDigitalCredentialEntity.class, digitalCredential.getId());
			RevocationRequestDigitalCredentialEntity secondCopy = secondManager.find(
					RevocationRequestDigitalCredentialEntity.class, digitalCredential.getId());
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
	void confirmsOnceWithoutCreatingRevocationOrReceipt() {
		RequestFixture fixture = request("70000001", 1);
		fixture.request().recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE);
		requestRepository.saveAndFlush(fixture.request());
		IdentityVerificationEntity verification = new IdentityVerificationEntity(
				fixture.request(), 1, "ID_PERU", NOW.minusSeconds(20), "identity-confirmation-it");
		verification.finish(IdentityVerificationStatus.VERIFIED, IdentityMatchResult.MATCH,
				NOW.minusSeconds(10), "identity-external", null, null, null, "ANA");
		identityRepository.saveAndFlush(verification);
		RevocationRequestDigitalCredentialEntity selected = digitalCredential(fixture, "ORD-CONFIRM",
				"a785da78-df44-44ea-a711-2eeceef114ad", 1);
		digitalCredentialRepository.saveAndFlush(selected);

		RevocationConfirmationRequest command = new RevocationConfirmationRequest(
				selected.getDigitalCredentialUuid(), selected.getStatusListIndex(), RevocationReasonCode.LOSS, null,
				true, RevocationConsentCatalog.VERSION);
		confirmationService.confirm(fixture.request().getId(), command, "confirmation-it");
		confirmationService.confirm(fixture.request().getId(), command, "confirmation-it-retry");

		DigitalCredentialRevocationRequestEntity confirmed = requestRepository.findById(
				fixture.request().getId()).orElseThrow();
		assertThat(confirmed.getRequestStatus()).isEqualTo(RevocationRequestStatus.CONFIRMED);
		assertThat(confirmed.getConsentVersion()).isEqualTo(RevocationConsentCatalog.VERSION);
		assertThat(auditRepository.findByRequest_IdOrderByOccurredAtAscIdAsc(confirmed.getId()))
				.filteredOn(event -> event.getEventType() == RevocationAuditEventType.CONSENT_CONFIRMED)
				.hasSize(1);
		assertThat(operationRepository.count()).isZero();
		assertThat(receiptRepository.count()).isZero();
	}

	@Test
	@Transactional
	void startsFreshConsultationAfterCompletedAtomicRevocationAndPreservesHistory() {
		RequestFixture historical = request("00000001", 1);
		RevocationRequestDigitalCredentialEntity selected = digitalCredential(historical, "ORD-HIST-1",
				"4d4aa8cc-3263-4c65-960f-a1823cc708af", 1);
		selected.select(NOW.plusSeconds(3));
		digitalCredentialRepository.saveAndFlush(selected);
		confirm(historical.request());
		RevocationOperationEntity operation = new RevocationOperationEntity(
				historical.request(), "idem-history-1", 1, NOW.plusSeconds(4), "corr-history");
		operation.markSubmitted(NOW.plusSeconds(5), "external-history");
		operation.complete(RevocationOperationStatus.SUCCEEDED, RevocationResult.SUCCEEDED,
				NOW.plusSeconds(6), NOW.plusSeconds(6), null, null);
		operationRepository.saveAndFlush(operation);
		selected.applyAtomicOutcome(RevocationResult.SUCCEEDED, NOW.plusSeconds(6));
		digitalCredentialRepository.saveAndFlush(selected);
		RevocationReceiptEntity receipt = new RevocationReceiptEntity(
				historical.request(), operation, "CD-HISTORY-001");
		receipt.markAvailable("receipts/history-001.pdf", NOW.plusSeconds(7), NOW.plusSeconds(8));
		receiptRepository.saveAndFlush(receipt);
		historical.request().transitionTo(RevocationRequestStatus.RECEIPT_AVAILABLE,
				RevocationFinalOutcome.REVOCATION_SUCCEEDED);
		requestRepository.saveAndFlush(historical.request());

		RevocationRequestResponse fresh = revocationRequestInitiationService.initiate(
				"00000001", "test-recaptcha-valid", "corr-fresh");

		assertThat(fresh.requestId()).isNotEqualTo(historical.request().getId());
		assertThat(requestRepository.findById(historical.request().getId())).get()
				.extracting(DigitalCredentialRevocationRequestEntity::getRequestStatus)
				.isEqualTo(RevocationRequestStatus.RECEIPT_AVAILABLE);
		assertThat(digitalCredentialRepository.findById(selected.getId())).isPresent();
		assertThat(operationRepository.findById(operation.getId())).isPresent();
		assertThat(receiptRepository.findById(receipt.getId())).isPresent();
	}

	private void assertAtomicOutcome(String dni, RevocationOperationStatus operationStatus, RevocationResult result,
			DigitalCredentialAvailabilityStatus digitalCredentialStatus, RevocationRequestStatus requestStatus,
			RevocationFinalOutcome finalOutcome) {
		RequestFixture fixture = request(dni, 1);
		RevocationRequestDigitalCredentialEntity first = digitalCredential(fixture, "ORD-1", uuidFor(dni, 1), 1);
		RevocationRequestDigitalCredentialEntity unselected = digitalCredential(fixture, "ORD-2", uuidFor(dni, 2), 2);
		first.select(NOW.plusSeconds(4));
		digitalCredentialRepository.saveAndFlush(first);
		confirm(fixture.request());
		RevocationOperationEntity operation = new RevocationOperationEntity(
				fixture.request(), "idem-" + dni, 1, NOW.plusSeconds(5), "corr-" + dni);
		operation.markSubmitted(NOW.plusSeconds(6), "external-" + dni);
		operation.complete(operationStatus, result, NOW.plusSeconds(7), NOW.plusSeconds(7),
				null, result == RevocationResult.FAILED ? "REJECTED" : null);
		operationRepository.saveAndFlush(operation);
		first.applyAtomicOutcome(result, NOW.plusSeconds(7));
		digitalCredentialRepository.saveAndFlush(first);
		fixture.request().transitionTo(requestStatus, finalOutcome);
		requestRepository.saveAndFlush(fixture.request());

		assertThat(digitalCredentialRepository.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(
				fixture.request().getId())).extracting(RevocationRequestDigitalCredentialEntity::getAvailabilityStatus)
				.containsOnly(digitalCredentialStatus);
		assertThat(digitalCredentialRepository.findById(unselected.getId())).get()
				.extracting(RevocationRequestDigitalCredentialEntity::getAvailabilityStatus)
				.isEqualTo(DigitalCredentialAvailabilityStatus.AVAILABLE);
		assertThat(digitalCredentialRepository.findById(first.getId())).get()
				.extracting(RevocationRequestDigitalCredentialEntity::getRevokedAt)
				.isEqualTo(result == RevocationResult.SUCCEEDED ? NOW.plusSeconds(7) : null);
		assertThat(operationRepository.findById(operation.getId())).get()
				.extracting(RevocationOperationEntity::getNormalizedResult).isEqualTo(result);
	}

	private void confirm(DigitalCredentialRevocationRequestEntity request) {
		request.confirmDecision(RevocationReasonCode.THEFT, null,
				Instant.now().plusSeconds(1), "REVOCACION_CREDENCIALES_DIGITALES_V1");
		requestRepository.saveAndFlush(request);
	}

	private RequestFixture request(String dni, int attempt) {
		DigitalCredentialRevocationRequestEntity request = requestRepository.saveAndFlush(
				new DigitalCredentialRevocationRequestEntity(dni));
		return new RequestFixture(request);
	}

	private DigitalCredentialRevocationRequestEntity identityVerifiedRequest(String dni) {
		DigitalCredentialRevocationRequestEntity request = requestRepository.saveAndFlush(
				new DigitalCredentialRevocationRequestEntity(dni));
		request.recordAvailability(pe.gob.reniec.credenciales.revocacion.revocation.persistence.CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.IDENTITY_VERIFIED);
		request = requestRepository.saveAndFlush(request);
		IdentityVerificationEntity verification = new IdentityVerificationEntity(
				request, 1, "ID_PERU", NOW.minusSeconds(20), "identity-listing-it-" + dni);
		verification.finish(IdentityVerificationStatus.VERIFIED, IdentityMatchResult.MATCH,
				NOW.minusSeconds(10), "identity-external-" + dni, null, null, null, "ANA");
		identityRepository.saveAndFlush(verification);
		return request;
	}

	private RevocationRequestDigitalCredentialEntity digitalCredential(RequestFixture fixture, String order,
			String uuid, long seconds) {
		RevocationRequestDigitalCredentialEntity digitalCredential = new RevocationRequestDigitalCredentialEntity(
				fixture.request(), Math.toIntExact(seconds), "DniPeruanoCredential",
				NOW.minus(1, ChronoUnit.DAYS), uuid, DigitalCredentialAvailabilityStatus.AVAILABLE,
				null, 0, NOW.plusSeconds(seconds));
		return digitalCredentialRepository.saveAndFlush(digitalCredential);
	}

	private String uuidFor(String dni, int suffix) {
		return "00000000-0000-4000-8000-" + dni + String.format("%04d", suffix);
	}

	private record RequestFixture(DigitalCredentialRevocationRequestEntity request) { }
}
