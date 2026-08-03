package pe.gob.reniec.credenciales.revocacion.revocation.initiation;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestStatus;

@Schema(description = "Resultado normalizado del inicio de una nueva solicitud de revocación.")
public record RevocationRequestResponse(
		@Schema(description = "Identificador interno de la solicitud. No funciona como credencial ni autorización.", example = "125", requiredMode = Schema.RequiredMode.REQUIRED)
		Long requestId,
		@Schema(description = "DNI enmascarado para presentación segura.", example = "******01", requiredMode = Schema.RequiredMode.REQUIRED)
		String maskedDni,
		@Schema(description = "Estado actual persistido de la solicitud.", example = "PENDING_IDENTITY_VERIFICATION", requiredMode = Schema.RequiredMode.REQUIRED)
		RevocationRequestStatus requestStatus,
		@Schema(description = "Resultado normalizado de la consulta inicial de existencia. No representa una lista detallada.", example = "AVAILABLE", requiredMode = Schema.RequiredMode.REQUIRED)
		AvailabilityOutcome availabilityResult,
		@Schema(description = "Indica si el backend autoriza continuar al siguiente paso.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
		boolean canContinue,
		@Schema(description = "Siguiente paso autorizado. Es nulo cuando no se permite continuar.", nullable = true,
				example = "IDENTITY_VERIFICATION", requiredMode = Schema.RequiredMode.REQUIRED)
		RevocationRequestNextStep nextStep) {
}
