package pe.gob.reniec.certificados.cancelacion.cancellation.initiation;

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

import pe.gob.reniec.certificados.cancelacion.cancellation.initiation.AvailabilityPersistenceCoordinator.AvailabilityPreparation;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateAvailabilityCheckEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateAvailabilityCheckRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CurrentAvailabilityResult;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.AvailabilityCheckStatus;

@ExtendWith(MockitoExtension.class)
class AvailabilityPersistenceCoordinatorTests {

	@Mock CertificateCancellationRequestRepository requests;
	@Mock CertificateAvailabilityCheckRepository checks;

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
	void abandonsReplaceableHistoryAndCreatesFreshRequestWithAttemptOne(CancellationRequestStatus status) {
		CertificateCancellationRequestEntity previous = request(1, status);
		when(requests.findTopByDniOrderByCreatedAtDesc("00000001")).thenReturn(Optional.of(previous));

		AvailabilityPreparation preparation = coordinator.prepare("00000001", "unit-correlation");

		assertThat(previous.getRequestStatus()).isEqualTo(CancellationRequestStatus.ABANDONED);
		assertThat(preparation.requestId()).isNotEqualTo(previous.getId());
		verify(checks).saveAndFlush(any(CertificateAvailabilityCheckEntity.class));
	}

	@ParameterizedTest
	@MethodSource("terminalStatuses")
	void preservesTerminalHistoryWhileCreatingFreshRequest(CancellationRequestStatus status) {
		CertificateCancellationRequestEntity previous = request(1, status);
		when(requests.findTopByDniOrderByCreatedAtDesc("00000001")).thenReturn(Optional.of(previous));

		AvailabilityPreparation preparation = coordinator.prepare("00000001", "unit-correlation");

		assertThat(previous.getRequestStatus()).isEqualTo(status);
		assertThat(preparation.requestId()).isNotEqualTo(previous.getId());
	}

	@ParameterizedTest
	@MethodSource("protectedStatuses")
	void blocksProtectedHistoryWithoutCreatingOrReturningIt(CancellationRequestStatus status) {
		CertificateCancellationRequestEntity previous = request(1, status);
		when(requests.findTopByDniOrderByCreatedAtDesc("00000001")).thenReturn(Optional.of(previous));

		assertThatThrownBy(() -> coordinator.prepare("00000001", "unit-correlation"))
				.isInstanceOf(CancellationRequestProtectedException.class);
		assertThat(previous.getRequestStatus()).isEqualTo(status);
		verify(checks, never()).saveAndFlush(any());
	}

	@Test
	void protectsLiveEligibilityAttempt() {
		CertificateCancellationRequestEntity previous = request(1, CancellationRequestStatus.CHECKING_AVAILABILITY);
		CertificateAvailabilityCheckEntity check = check(previous, 11, Instant.now());
		when(requests.findTopByDniOrderByCreatedAtDesc("00000001")).thenReturn(Optional.of(previous));
		when(checks.findFirstByRequest_IdOrderByAttemptNumberDesc(1L)).thenReturn(Optional.of(check));

		assertThatThrownBy(() -> coordinator.prepare("00000001", "unit-correlation"))
				.isInstanceOf(AvailabilityCheckInProgressException.class);
		assertThat(previous.getRequestStatus()).isEqualTo(CancellationRequestStatus.CHECKING_AVAILABILITY);
	}

	@Test
	void closesStaleAttemptAbandonsOldRequestAndStartsFreshAttemptOne() {
		CertificateCancellationRequestEntity previous = request(1, CancellationRequestStatus.CHECKING_AVAILABILITY);
		CertificateAvailabilityCheckEntity stale = check(previous, 11, Instant.now().minusSeconds(60));
		when(requests.findTopByDniOrderByCreatedAtDesc("00000001")).thenReturn(Optional.of(previous));
		when(checks.findFirstByRequest_IdOrderByAttemptNumberDesc(1L)).thenReturn(Optional.of(stale));

		AvailabilityPreparation preparation = coordinator.prepare("00000001", "unit-correlation");

		assertThat(previous.getRequestStatus()).isEqualTo(CancellationRequestStatus.ABANDONED);
		assertThat(stale.getCheckStatus()).isEqualTo(pe.gob.reniec.certificados.cancelacion.cancellation.persistence.AvailabilityCheckStatus.FAILED);
		assertThat(preparation.requestId()).isNotEqualTo(previous.getId());
	}

	@Test
	void rejectsLateFinalizationAfterRequestWasAbandoned() {
		CertificateCancellationRequestEntity old = request(1, CancellationRequestStatus.CHECKING_AVAILABILITY);
		CertificateAvailabilityCheckEntity attempt = check(old, 11, Instant.now().minusSeconds(60));
		old.transitionTo(CancellationRequestStatus.ABANDONED, null);
		when(requests.findByIdForUpdate(1L)).thenReturn(Optional.of(old));
		when(checks.findByIdForUpdate(11L)).thenReturn(Optional.of(attempt));

		assertThatThrownBy(() -> coordinator.finalizeAttempt(new AvailabilityPreparation(1L, 11L),
				new AvailabilityResult(AvailabilityOutcome.AVAILABLE, "late", null)))
				.isInstanceOf(CancellationRequestConcurrencyException.class);
		assertThat(old.getRequestStatus()).isEqualTo(CancellationRequestStatus.ABANDONED);
		assertThat(attempt.getCheckStatus()).isEqualTo(AvailabilityCheckStatus.SUBMITTED);
	}

	private CertificateCancellationRequestEntity request(long id, CancellationRequestStatus status) {
		CertificateCancellationRequestEntity request = new CertificateCancellationRequestEntity("00000001");
		request.transitionTo(status, null);
		ReflectionTestUtils.setField(request, "id", id);
		return request;
	}

	private CertificateAvailabilityCheckEntity check(CertificateCancellationRequestEntity request, long id,
			Instant requestedAt) {
		CertificateAvailabilityCheckEntity check = new CertificateAvailabilityCheckEntity(
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

	static Stream<CancellationRequestStatus> replaceableStatuses() {
		return Stream.of(
				CancellationRequestStatus.STARTED,
				CancellationRequestStatus.CERTIFICATES_AVAILABLE,
				CancellationRequestStatus.PENDING_IDENTITY_VERIFICATION,
				CancellationRequestStatus.IDENTITY_VERIFIED,
				CancellationRequestStatus.AUTHENTICATED_PENDING_CERTIFICATE_LIST,
				CancellationRequestStatus.CERTIFICATES_SELECTED,
				CancellationRequestStatus.REASON_REGISTERED,
				CancellationRequestStatus.PENDING_CONFIRMATION);
	}

	static Stream<CancellationRequestStatus> terminalStatuses() {
		return Stream.of(
				CancellationRequestStatus.NO_CERTIFICATES_AVAILABLE,
				CancellationRequestStatus.REVOCATION_SUCCEEDED,
				CancellationRequestStatus.REVOCATION_FAILED,
				CancellationRequestStatus.COMPLETED,
				CancellationRequestStatus.FAILED,
				CancellationRequestStatus.RECEIPT_AVAILABLE,
				CancellationRequestStatus.ABANDONED);
	}

	static Stream<CancellationRequestStatus> protectedStatuses() {
		return Stream.of(
				CancellationRequestStatus.CONFIRMED,
				CancellationRequestStatus.REVOCATION_IN_PROGRESS,
				CancellationRequestStatus.REVOCATION_OUTCOME_UNKNOWN,
				CancellationRequestStatus.OUTCOME_UNKNOWN);
	}
}
