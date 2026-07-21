package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus;

@Schema(description = "Resultado normalizado del inicio de una nueva solicitud de cancelación.")
public record CancellationRequestResponse(
		@Schema(description = "Identificador interno de la solicitud. No funciona como credencial ni autorización.", example = "125", requiredMode = Schema.RequiredMode.REQUIRED)
		Long requestId,
		@Schema(description = "DNI enmascarado para presentación segura.", example = "******01", requiredMode = Schema.RequiredMode.REQUIRED)
		String maskedDni,
		@Schema(description = "Estado actual persistido de la solicitud.", example = "PENDING_IDENTITY_VERIFICATION", requiredMode = Schema.RequiredMode.REQUIRED)
		CancellationRequestStatus requestStatus,
		@Schema(description = "Resultado normalizado de la consulta de certificados.", example = "ELIGIBLE", requiredMode = Schema.RequiredMode.REQUIRED)
		EligibilityOutcome eligibilityResult,
		@Schema(description = "Indica si el backend autoriza continuar al siguiente paso.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
		boolean canContinue,
		@Schema(description = "Siguiente paso autorizado. Es nulo cuando no se permite continuar.", nullable = true,
				example = "IDENTITY_VERIFICATION", requiredMode = Schema.RequiredMode.REQUIRED)
		EligibilityNextStep nextStep) {
}
