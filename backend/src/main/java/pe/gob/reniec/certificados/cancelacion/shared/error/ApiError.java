package pe.gob.reniec.certificados.cancelacion.shared.error;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Formato común y seguro de los errores controlados de la API.")
public record ApiError(
		@Schema(description = "Código público y estable del error.", example = "VALIDATION_ERROR", requiredMode = Schema.RequiredMode.REQUIRED)
		String code,
		@Schema(description = "Mensaje comprensible sin detalles internos.", example = "La solicitud contiene datos inválidos.", requiredMode = Schema.RequiredMode.REQUIRED)
		String message,
		@Schema(description = "Fecha y hora UTC del error.", type = "string", format = "date-time",
				example = "2026-07-20T18:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
		Instant timestamp,
		@Schema(description = "Ruta de la solicitud que produjo el error.", example = "/api/v1/cancellation-requests", requiredMode = Schema.RequiredMode.REQUIRED)
		String path,
		@Schema(description = "Identificador de correlación para soporte y trazabilidad.",
				example = "7a5f3f75-3bd2-4c47-90fc-6cfc79f1ec2d", requiredMode = Schema.RequiredMode.REQUIRED)
		String correlationId) {
}
