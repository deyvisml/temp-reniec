package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus;

public record CancellationRequestResponse(
		Long requestId,
		String maskedDni,
		CancellationRequestStatus requestStatus,
		EligibilityOutcome eligibilityResult,
		boolean canContinue,
		EligibilityNextStep nextStep,
		boolean reused) {
}
