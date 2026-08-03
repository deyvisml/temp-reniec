package pe.gob.reniec.credenciales.revocacion.revocation.initiation;

import java.util.EnumSet;
import java.util.Set;

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestStatus;

final class RevocationRequestInitiationPolicy {

	private static final Set<RevocationRequestStatus> REPLACEABLE_PRE_CONFIRMATION = EnumSet.of(
			RevocationRequestStatus.STARTED,
			RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION,
			RevocationRequestStatus.IDENTITY_VERIFIED,
			RevocationRequestStatus.AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST,
			RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE,
			RevocationRequestStatus.DIGITAL_CREDENTIALS_SELECTED,
			RevocationRequestStatus.REASON_REGISTERED,
			RevocationRequestStatus.PENDING_CONFIRMATION);

	private static final Set<RevocationRequestStatus> PROTECTED = EnumSet.of(
			RevocationRequestStatus.CONFIRMED,
			RevocationRequestStatus.REVOCATION_IN_PROGRESS,
			RevocationRequestStatus.REVOCATION_OUTCOME_UNKNOWN,
			RevocationRequestStatus.OUTCOME_UNKNOWN);

	private static final Set<RevocationRequestStatus> TERMINAL_HISTORY = EnumSet.of(
			RevocationRequestStatus.NO_DIGITAL_CREDENTIALS_AVAILABLE,
			RevocationRequestStatus.REVOCATION_SUCCEEDED,
			RevocationRequestStatus.REVOCATION_FAILED,
			RevocationRequestStatus.COMPLETED,
			RevocationRequestStatus.FAILED,
			RevocationRequestStatus.RECEIPT_AVAILABLE,
			RevocationRequestStatus.ABANDONED);

	private RevocationRequestInitiationPolicy() {
	}

	static boolean isReplaceable(RevocationRequestStatus status) {
		return REPLACEABLE_PRE_CONFIRMATION.contains(status);
	}

	static boolean isAvailabilityCheckInProgress(RevocationRequestStatus status) {
		return status == RevocationRequestStatus.CHECKING_AVAILABILITY;
	}

	static boolean isProtected(RevocationRequestStatus status) {
		return PROTECTED.contains(status);
	}

	static boolean isTerminalHistory(RevocationRequestStatus status) {
		return TERMINAL_HISTORY.contains(status);
	}
}
