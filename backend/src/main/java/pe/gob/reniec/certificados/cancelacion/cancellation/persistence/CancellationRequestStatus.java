package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado actual de la solicitud ciudadana de cancelación.")
public enum CancellationRequestStatus {
	STARTED,
	CHECKING_ELIGIBILITY,
	NOT_ELIGIBLE,
	ELIGIBLE,
	PENDING_IDENTITY_VERIFICATION,
	IDENTITY_VERIFIED,
	REASON_REGISTERED,
	PENDING_CONFIRMATION,
	CONFIRMED,
	REVOCATION_IN_PROGRESS,
	COMPLETED,
	FAILED,
	OUTCOME_UNKNOWN,
	RECEIPT_AVAILABLE,
	ABANDONED
}
