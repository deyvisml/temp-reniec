package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class DigitalCredentialAvailabilityCheckEntityTests {

	private static final Instant REQUESTED_AT = Instant.parse("2026-07-21T15:00:00Z");

	@Test
	void acceptsOnlyCoherentTerminalTransitions() {
		DigitalCredentialAvailabilityCheckEntity completed = submittedCheck();
		completed.complete(AvailabilityCheckResult.AVAILABLE, REQUESTED_AT.plusSeconds(1), " provider-ref ");

		assertThat(completed.getCheckStatus()).isEqualTo(AvailabilityCheckStatus.COMPLETED);
		assertThat(completed.getExternalReference()).isEqualTo("provider-ref");
		assertThat(completed.getErrorCode()).isNull();
		assertThatThrownBy(() -> completed.fail(
				AvailabilityCheckResult.ERROR, REQUESTED_AT.plusSeconds(2), "LATE_ERROR"))
				.isInstanceOf(IllegalStateException.class);

		DigitalCredentialAvailabilityCheckEntity failed = submittedCheck();
		failed.fail(AvailabilityCheckResult.ERROR, REQUESTED_AT.plusSeconds(1), " PROVIDER_ERROR ");

		assertThat(failed.getCheckStatus()).isEqualTo(AvailabilityCheckStatus.FAILED);
		assertThat(failed.getErrorCode()).isEqualTo("PROVIDER_ERROR");
		assertThat(failed.getExternalReference()).isNull();
		assertThatThrownBy(() -> failed.complete(
				AvailabilityCheckResult.AVAILABLE, REQUESTED_AT.plusSeconds(2), "late-ref"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void rejectsMismatchedResultsWithoutMutatingTheAttempt() {
		DigitalCredentialAvailabilityCheckEntity check = submittedCheck();

		assertThatThrownBy(() -> check.complete(
				AvailabilityCheckResult.ERROR, REQUESTED_AT.plusSeconds(1), null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(check.getCheckStatus()).isEqualTo(AvailabilityCheckStatus.SUBMITTED);
		assertThat(check.getNormalizedResult()).isNull();

		assertThatThrownBy(() -> check.fail(
				AvailabilityCheckResult.NOT_AVAILABLE, REQUESTED_AT.plusSeconds(1), "INVALID"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(check.getCheckStatus()).isEqualTo(AvailabilityCheckStatus.SUBMITTED);
		assertThat(check.getNormalizedResult()).isNull();
	}

	private DigitalCredentialAvailabilityCheckEntity submittedCheck() {
		return new DigitalCredentialAvailabilityCheckEntity(
				new DigitalCredentialRevocationRequestEntity("00000001"),
				1,
				AvailabilityCheckStatus.SUBMITTED,
				REQUESTED_AT,
				"unit-correlation");
	}
}
