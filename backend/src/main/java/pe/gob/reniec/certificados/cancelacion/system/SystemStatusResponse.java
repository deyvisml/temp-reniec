package pe.gob.reniec.certificados.cancelacion.system;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado técnico del backend y su conexión con MySQL")
public record SystemStatusResponse(
		@Schema(example = "UP") String status,
		@Schema(example = "UP") String database,
		@Schema(type = "string", format = "date-time") Instant timestamp) {
}
