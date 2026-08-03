package pe.gob.reniec.credenciales.revocacion.revocation.confirmation;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReasonCode;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestStatus;

@Schema(description = "Resumen autoritativo y minimizado de la revocación.")
public record RevocationReviewResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) RevocationRequestStatus requestStatus,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "******91") String maskedDni,
		@Schema(description = "Primer nombre verificado por ID Perú; ausente solo en evidencia histórica.",
				maxLength = 100, example = "ANA") String firstName,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) SelectedDigitalCredential digitalCredential,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) RevocationReasonCode reasonCode,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reasonLabel,
		@Schema(description = "Descripción validada para el motivo OTHER.") String otherReason,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> consequences,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String consentText,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 64) String consentVersion,
		@Schema(format = "date-time", description = "Fecha UTC persistida; ausente antes de confirmar.") Instant confirmedAt,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean confirmed) {

	@Schema(description = "Credencial identificado por datos visibles, sin exponer su UUID.")
	public record SelectedDigitalCredential(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int statusListIndex,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant emissionCreatedAt) { }
}
