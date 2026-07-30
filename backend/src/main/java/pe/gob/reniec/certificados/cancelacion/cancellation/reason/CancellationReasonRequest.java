package pe.gob.reniec.certificados.cancelacion.cancellation.reason;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReasonCode;

public record CancellationReasonRequest(
		@NotNull
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Código controlado del motivo.")
		CancellationReasonCode reasonCode,
		@Size(max = 300)
		@Schema(description = "Descripción requerida únicamente cuando el motivo es OTHER.", maxLength = 300)
		String otherReason) {
}
