package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import java.util.EnumSet;
import java.util.Set;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus;

final class CancellationRequestInitiationPolicy {

	private static final Set<CancellationRequestStatus> REPLACEABLE_PRE_CONFIRMATION = EnumSet.of(
			CancellationRequestStatus.STARTED,
			CancellationRequestStatus.PENDING_IDENTITY_VERIFICATION,
			CancellationRequestStatus.IDENTITY_VERIFIED,
			CancellationRequestStatus.AUTHENTICATED_PENDING_CERTIFICATE_LIST,
			CancellationRequestStatus.CERTIFICATES_AVAILABLE,
			CancellationRequestStatus.CERTIFICATES_SELECTED,
			CancellationRequestStatus.REASON_REGISTERED,
			CancellationRequestStatus.PENDING_CONFIRMATION);

	private static final Set<CancellationRequestStatus> PROTECTED = EnumSet.of(
			CancellationRequestStatus.CONFIRMED,
			CancellationRequestStatus.REVOCATION_IN_PROGRESS,
			CancellationRequestStatus.REVOCATION_OUTCOME_UNKNOWN,
			CancellationRequestStatus.OUTCOME_UNKNOWN);

	private static final Set<CancellationRequestStatus> TERMINAL_HISTORY = EnumSet.of(
			CancellationRequestStatus.NO_CERTIFICATES_AVAILABLE,
			CancellationRequestStatus.REVOCATION_SUCCEEDED,
			CancellationRequestStatus.REVOCATION_FAILED,
			CancellationRequestStatus.COMPLETED,
			CancellationRequestStatus.FAILED,
			CancellationRequestStatus.RECEIPT_AVAILABLE,
			CancellationRequestStatus.ABANDONED);

	private CancellationRequestInitiationPolicy() {
	}

	static boolean isReplaceable(CancellationRequestStatus status) {
		return REPLACEABLE_PRE_CONFIRMATION.contains(status);
	}

	static boolean isEligibilityInProgress(CancellationRequestStatus status) {
		return status == CancellationRequestStatus.CHECKING_AVAILABILITY;
	}

	static boolean isProtected(CancellationRequestStatus status) {
		return PROTECTED.contains(status);
	}

	static boolean isTerminalHistory(CancellationRequestStatus status) {
		return TERMINAL_HISTORY.contains(status);
	}
}
