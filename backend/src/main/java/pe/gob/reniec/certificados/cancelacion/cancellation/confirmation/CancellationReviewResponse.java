package pe.gob.reniec.certificados.cancelacion.cancellation.confirmation;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReasonCode;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus;

@Schema(description = "Resumen autoritativo y minimizado de la cancelación.")
public record CancellationReviewResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) CancellationRequestStatus requestStatus,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "******91") String maskedDni,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) SelectedCertificate certificate,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) CancellationReasonCode reasonCode,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reasonLabel,
		@Schema(description = "Descripción validada para el motivo OTHER.") String otherReason,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> consequences,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String consentText,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 64) String consentVersion,
		@Schema(format = "date-time", description = "Fecha UTC persistida; ausente antes de confirmar.") Instant confirmedAt,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean confirmed) {

	@Schema(description = "Certificado identificado por datos visibles, sin exponer su UUID.")
	public record SelectedCertificate(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 64) String orderNumber,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant emissionCreatedAt) { }
}
