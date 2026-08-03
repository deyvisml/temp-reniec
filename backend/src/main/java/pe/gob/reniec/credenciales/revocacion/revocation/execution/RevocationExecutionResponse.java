package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.ReceiptGenerationStatus;

@Schema(description = "Resultado ciudadano de la revocacion sin exponer identificadores sensibles.")
public record RevocationExecutionResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) RevocationExecutionState state,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) RevocationRequestStatus requestStatus,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "******91") String maskedDni,
		@Schema(description = "Primer nombre verificado por ID Perú; ausente solo en evidencia histórica.",
				maxLength = 100, example = "ANA") String firstName,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) DigitalCredential digitalCredential,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reasonLabel,
		String otherReason,
		Instant confirmedAt,
		Instant completedAt,
		Processing processing,
		Receipt receipt) {

	public record DigitalCredential(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int statusListIndex,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time")
			Instant emissionCreatedAt) { }

	public record Receipt(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) ReceiptGenerationStatus status,
			Instant availableAt,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean downloadAvailable) { }

	public record Processing(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) RevocationProcessingPhase phase,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant startedAt,
			@Schema(format = "date-time") Instant readyAt,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant serverTime) { }
}
