package pe.gob.reniec.certificados.cancelacion.cancellation.execution;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.ReceiptGenerationStatus;

@Schema(description = "Resultado ciudadano de la cancelacion sin exponer identificadores sensibles.")
public record CancellationExecutionResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) CancellationExecutionState state,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) CancellationRequestStatus requestStatus,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "******91") String maskedDni,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Certificate certificate,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reasonLabel,
		String otherReason,
		Instant confirmedAt,
		Instant completedAt,
		Receipt receipt) {

	public record Certificate(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orderNumber,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
			Instant emissionCreatedAt) { }

	public record Receipt(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) ReceiptGenerationStatus status,
			Instant availableAt,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean downloadAvailable) { }
}
