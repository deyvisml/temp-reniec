package pe.gob.reniec.certificados.cancelacion.cancellation.initiation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado normalizado de la consulta inicial de existencia de certificados.")
public enum AvailabilityOutcome { AVAILABLE, NOT_AVAILABLE, INCONCLUSIVE, UNAVAILABLE, ERROR }
