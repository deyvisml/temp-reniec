package pe.gob.reniec.credenciales.revocacion.revocation.initiation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import pe.gob.reniec.credenciales.revocacion.revocation.initiation.AvailabilityPersistenceCoordinator.AvailabilityPreparation;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialAvailabilityCheckEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialAvailabilityCheckRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.CurrentAvailabilityResult;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.AvailabilityCheckStatus;

@ExtendWith(MockitoExtension.class)
class AvailabilityPersistenceCoordinatorTests {

	@Mock DigitalCredentialRevocationRequestRepository requests;
	@Mock DigitalCredentialAvailabilityCheckRepository checks;

	AvailabilityPersistenceCoordinator coordinator;
	AtomicLong ids;

	@BeforeEach
	void setUp() {
		coordinator = new AvailabilityPersistenceCoordinator(requests, checks, Duration.ofSeconds(30));
		ids = new AtomicLong(100);
		lenient().when(requests.saveAndFlush(any())).thenAnswer(invocation -> assignId(invocation.getArgument(0)));
		lenient().when(checks.saveAndFlush(any())).thenAnswer(invocation -> assignId(invocation.getArgument(0)));
	}

	@ParameterizedTest
	@MethodSource("replaceableStatuses")
	void abandonsReplaceableHistoryAndCreatesFreshRequestWithAttemptOne(RevocationRequestStatus status) {
		DigitalCredentialRevocationRequestEntity previous = request(1, status);
		when(requests.findTopByDniOrderByCreatedAtDesc("00000001")).thenReturn(Optional.of(previous));

		AvailabilityPreparation preparation = coordinator.prepare("00000001", "unit-correlation");

		assertThat(previous.getRequestStatus()).isEqualTo(RevocationRequestStatus.ABANDONED);
		assertThat(preparation.requestId()).isNotEqualTo(previous.getId());
		verify(checks).saveAndFlush(any(DigitalCredentialAvailabilityCheckEntity.class));
	}

	@ParameterizedTest
	@MethodSource("terminalStatuses")
	void preservesTerminalHistoryWhileCreatingFreshRequest(RevocationRequestStatus status) {
		DigitalCredentialRevocationRequestEntity previous = request(1, status);
		when(requests.findTopByDniOrderByCreatedAtDesc("00000001")).thenReturn(Optional.of(previous));

		AvailabilityPreparation preparation = coordinator.prepare("00000001", "unit-correlation");

		assertThat(previous.getRequestStatus()).isEqualTo(status);
		assertThat(preparation.requestId()).isNotEqualTo(previous.getId());
	}

	@ParameterizedTest
	@MethodSource("protectedStatuses")
	void blocksProtectedHistoryWithoutCreatingOrReturningIt(RevocationRequestStatus status) {
		DigitalCredentialRevocationRequestEntity previous = request(1, status);
		when(requests.findTopByDniOrderByCreatedAtDesc("00000001")).thenReturn(Optional.of(previous));

		assertThatThrownBy(() -> coordinator.prepare("00000001", "unit-correlation"))
				.isInstanceOf(RevocationRequestProtectedException.class);
		assertThat(previous.getRequestStatus()).isEqualTo(status);
		verify(checks, never()).saveAndFlush(any());
	}

	@Test
	void protectsLiveEligibilityAttempt() {
		DigitalCredentialRevocationRequestEntity previous = request(1, RevocationRequestStatus.CHECKING_AVAILABILITY);
		DigitalCredentialAvailabilityCheckEntity check = check(previous, 11, Instant.now());
		when(requests.findTopByDniOrderByCreatedAtDesc("00000001")).thenReturn(Optional.of(previous));
		when(checks.findFirstByRequest_IdOrderByAttemptNumberDesc(1L)).thenReturn(Optional.of(check));

		assertThatThrownBy(() -> coordinator.prepare("00000001", "unit-correlation"))
				.isInstanceOf(AvailabilityCheckInProgressException.class);
		assertThat(previous.getRequestStatus()).isEqualTo(RevocationRequestStatus.CHECKING_AVAILABILITY);
	}

	@Test
	void closesStaleAttemptAbandonsOldRequestAndStartsFreshAttemptOne() {
		DigitalCredentialRevocationRequestEntity previous = request(1, RevocationRequestStatus.CHECKING_AVAILABILITY);
		DigitalCredentialAvailabilityCheckEntity stale = check(previous, 11, Instant.now().minusSeconds(60));
		when(requests.findTopByDniOrderByCreatedAtDesc("00000001")).thenReturn(Optional.of(previous));
		when(checks.findFirstByRequest_IdOrderByAttemptNumberDesc(1L)).thenReturn(Optional.of(stale));

		AvailabilityPreparation preparation = coordinator.prepare("00000001", "unit-correlation");

		assertThat(previous.getRequestStatus()).isEqualTo(RevocationRequestStatus.ABANDONED);
		assertThat(stale.getCheckStatus()).isEqualTo(pe.gob.reniec.credenciales.revocacion.revocation.persistence.AvailabilityCheckStatus.FAILED);
		assertThat(preparation.requestId()).isNotEqualTo(previous.getId());
	}

	@Test
	void rejectsLateFinalizationAfterRequestWasAbandoned() {
		DigitalCredentialRevocationRequestEntity old = request(1, RevocationRequestStatus.CHECKING_AVAILABILITY);
		DigitalCredentialAvailabilityCheckEntity attempt = check(old, 11, Instant.now().minusSeconds(60));
		old.transitionTo(RevocationRequestStatus.ABANDONED, null);
		when(requests.findByIdForUpdate(1L)).thenReturn(Optional.of(old));
		when(checks.findByIdForUpdate(11L)).thenReturn(Optional.of(attempt));

		assertThatThrownBy(() -> coordinator.finalizeAttempt(new AvailabilityPreparation(1L, 11L),
				new AvailabilityResult(AvailabilityOutcome.AVAILABLE, "late", null)))
				.isInstanceOf(RevocationRequestConcurrencyException.class);
		assertThat(old.getRequestStatus()).isEqualTo(RevocationRequestStatus.ABANDONED);
		assertThat(attempt.getCheckStatus()).isEqualTo(AvailabilityCheckStatus.SUBMITTED);
	}

	private DigitalCredentialRevocationRequestEntity request(long id, RevocationRequestStatus status) {
		DigitalCredentialRevocationRequestEntity request = new DigitalCredentialRevocationRequestEntity("00000001");
		request.transitionTo(status, null);
		ReflectionTestUtils.setField(request, "id", id);
		return request;
	}

	private DigitalCredentialAvailabilityCheckEntity check(DigitalCredentialRevocationRequestEntity request, long id,
			Instant requestedAt) {
		DigitalCredentialAvailabilityCheckEntity check = new DigitalCredentialAvailabilityCheckEntity(
				request, 1, AvailabilityCheckStatus.SUBMITTED, requestedAt, "unit-correlation");
		ReflectionTestUtils.setField(check, "id", id);
		return check;
	}

	private <T> T assignId(T entity) {
		if (ReflectionTestUtils.getField(entity, "id") == null) {
			ReflectionTestUtils.setField(entity, "id", ids.incrementAndGet());
		}
		return entity;
	}

	static Stream<RevocationRequestStatus> replaceableStatuses() {
		return Stream.of(
				RevocationRequestStatus.STARTED,
				RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE,
				RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION,
				RevocationRequestStatus.IDENTITY_VERIFIED,
				RevocationRequestStatus.AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST,
				RevocationRequestStatus.DIGITAL_CREDENTIALS_SELECTED,
				RevocationRequestStatus.REASON_REGISTERED,
				RevocationRequestStatus.PENDING_CONFIRMATION);
	}

	static Stream<RevocationRequestStatus> terminalStatuses() {
		return Stream.of(
				RevocationRequestStatus.NO_DIGITAL_CREDENTIALS_AVAILABLE,
				RevocationRequestStatus.REVOCATION_SUCCEEDED,
				RevocationRequestStatus.REVOCATION_FAILED,
				RevocationRequestStatus.COMPLETED,
				RevocationRequestStatus.FAILED,
				RevocationRequestStatus.RECEIPT_AVAILABLE,
				RevocationRequestStatus.ABANDONED);
	}

	static Stream<RevocationRequestStatus> protectedStatuses() {
		return Stream.of(
				RevocationRequestStatus.CONFIRMED,
				RevocationRequestStatus.REVOCATION_IN_PROGRESS,
				RevocationRequestStatus.REVOCATION_OUTCOME_UNKNOWN,
				RevocationRequestStatus.OUTCOME_UNKNOWN);
	}
}
