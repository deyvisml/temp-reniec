package pe.gob.reniec.certificados.cancelacion.shared.error;

import java.time.Instant;

public record ApiError(
		String code,
		String message,
		Instant timestamp,
		String path,
		String correlationId) {
}
