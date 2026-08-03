package pe.gob.reniec.credenciales.revocacion.system;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado técnico del backend y su conexión con MySQL")
public record SystemStatusResponse(
		@Schema(description = "Estado agregado del backend.", example = "UP", requiredMode = Schema.RequiredMode.REQUIRED) String status,
		@Schema(description = "Disponibilidad de la conexión comprobada con MySQL.", example = "UP", requiredMode = Schema.RequiredMode.REQUIRED) String database,
		@Schema(description = "Fecha y hora UTC de la comprobación.", type = "string", format = "date-time",
				example = "2026-07-20T18:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED) Instant timestamp) {
}
