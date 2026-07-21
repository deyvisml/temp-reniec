package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

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

import pe.gob.reniec.certificados.cancelacion.cancellation.eligibility.EligibilityPersistenceCoordinator.EligibilityPreparation;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateEligibilityCheckEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateEligibilityCheckRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CurrentEligibilityResult;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.EligibilityCheckStatus;

@ExtendWith(MockitoExtension.class)
class EligibilityPersistenceCoordinatorTests {

	@Mock CertificateCancellationRequestRepository requests;
	@Mock CertificateEligibilityCheckRepository checks;

	EligibilityPersistenceCoordinator coordinator;
	AtomicLong ids;

	@BeforeEach
	void setUp() {
		coordinator = new EligibilityPersistenceCoordinator(requests, checks, Duration.ofSeconds(30));
		ids = new AtomicLong(100);
		lenient().when(requests.saveAndFlush(any())).thenAnswer(invocation -> assignId(invocation.getArgument(0)));
		lenient().when(checks.saveAndFlush(any())).thenAnswer(invocation -> assignId(invocation.getArgument(0)));
	}

	@ParameterizedTest
	@MethodSource("replaceableStatuses")
	void abandonsReplaceableHistoryAndCreatesFreshRequestWithAttemptOne(CancellationRequestStatus status) {
		CertificateCancellationRequestEntity previous = request(1, status);
		when(requests.findTopByDniOrderByCreatedAtDesc("00000001")).thenReturn(Optional.of(previous));

		EligibilityPreparation preparation = coordinator.prepare("00000001", "unit-correlation");

		assertThat(previous.getRequestStatus()).isEqualTo(CancellationRequestStatus.ABANDONED);
		assertThat(preparation.requestId()).isNotEqualTo(previous.getId());
		verify(checks).saveAndFlush(any(CertificateEligibilityCheckEntity.class));
	}

	@ParameterizedTest
	@MethodSource("terminalStatuses")
	void preservesTerminalHistoryWhileCreatingFreshRequest(CancellationRequestStatus status) {
		CertificateCancellationRequestEntity previous = request(1, status);
		when(requests.findTopByDniOrderByCreatedAtDesc("00000001")).thenReturn(Optional.of(previous));

		EligibilityPreparation preparation = coordinator.prepare("00000001", "unit-correlation");

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
		CertificateCancellationRequestEntity previous = request(1, CancellationRequestStatus.CHECKING_ELIGIBILITY);
		CertificateEligibilityCheckEntity check = check(previous, 11, Instant.now());
		when(requests.findTopByDniOrderByCreatedAtDesc("00000001")).thenReturn(Optional.of(previous));
		when(checks.findFirstByRequest_IdOrderByAttemptNumberDesc(1L)).thenReturn(Optional.of(check));

		assertThatThrownBy(() -> coordinator.prepare("00000001", "unit-correlation"))
				.isInstanceOf(EligibilityInProgressException.class);
		assertThat(previous.getRequestStatus()).isEqualTo(CancellationRequestStatus.CHECKING_ELIGIBILITY);
	}

	@Test
	void closesStaleAttemptAbandonsOldRequestAndStartsFreshAttemptOne() {
		CertificateCancellationRequestEntity previous = request(1, CancellationRequestStatus.CHECKING_ELIGIBILITY);
		CertificateEligibilityCheckEntity stale = check(previous, 11, Instant.now().minusSeconds(60));
		when(requests.findTopByDniOrderByCreatedAtDesc("00000001")).thenReturn(Optional.of(previous));
		when(checks.findFirstByRequest_IdOrderByAttemptNumberDesc(1L)).thenReturn(Optional.of(stale));

		EligibilityPreparation preparation = coordinator.prepare("00000001", "unit-correlation");

		assertThat(previous.getRequestStatus()).isEqualTo(CancellationRequestStatus.ABANDONED);
		assertThat(stale.getCheckStatus()).isEqualTo(pe.gob.reniec.certificados.cancelacion.cancellation.persistence.EligibilityCheckStatus.FAILED);
		assertThat(preparation.requestId()).isNotEqualTo(previous.getId());
	}

	@Test
	void rejectsLateFinalizationAfterRequestWasAbandoned() {
		CertificateCancellationRequestEntity old = request(1, CancellationRequestStatus.CHECKING_ELIGIBILITY);
		CertificateEligibilityCheckEntity attempt = check(old, 11, Instant.now().minusSeconds(60));
		old.transitionTo(CancellationRequestStatus.ABANDONED, null);
		when(requests.findByIdForUpdate(1L)).thenReturn(Optional.of(old));
		when(checks.findByIdForUpdate(11L)).thenReturn(Optional.of(attempt));

		assertThatThrownBy(() -> coordinator.finalizeAttempt(new EligibilityPreparation(1L, 11L),
				new EligibilityGatewayResult(EligibilityOutcome.ELIGIBLE, "late", null)))
				.isInstanceOf(EligibilityConcurrencyException.class);
		assertThat(old.getRequestStatus()).isEqualTo(CancellationRequestStatus.ABANDONED);
		assertThat(attempt.getCheckStatus()).isEqualTo(EligibilityCheckStatus.SUBMITTED);
	}

	private CertificateCancellationRequestEntity request(long id, CancellationRequestStatus status) {
		CertificateCancellationRequestEntity request = new CertificateCancellationRequestEntity("00000001");
		request.transitionTo(status, null);
		ReflectionTestUtils.setField(request, "id", id);
		return request;
	}

	private CertificateEligibilityCheckEntity check(CertificateCancellationRequestEntity request, long id,
			Instant requestedAt) {
		CertificateEligibilityCheckEntity check = new CertificateEligibilityCheckEntity(
				request, 1, EligibilityCheckStatus.SUBMITTED, requestedAt, "unit-correlation");
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
				CancellationRequestStatus.ELIGIBLE,
				CancellationRequestStatus.PENDING_IDENTITY_VERIFICATION,
				CancellationRequestStatus.IDENTITY_VERIFIED,
				CancellationRequestStatus.AUTHENTICATED_PENDING_SELECTION,
				CancellationRequestStatus.CERTIFICATES_SELECTED,
				CancellationRequestStatus.REASON_REGISTERED,
				CancellationRequestStatus.PENDING_CONFIRMATION);
	}

	static Stream<CancellationRequestStatus> terminalStatuses() {
		return Stream.of(
				CancellationRequestStatus.NO_CERTIFICATES_AVAILABLE,
				CancellationRequestStatus.NOT_ELIGIBLE,
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
