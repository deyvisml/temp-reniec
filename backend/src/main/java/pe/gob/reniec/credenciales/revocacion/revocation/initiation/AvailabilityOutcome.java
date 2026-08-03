package pe.gob.reniec.credenciales.revocacion.revocation.initiation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado normalizado de la consulta inicial de existencia de credenciales.")
public enum AvailabilityOutcome { AVAILABLE, NOT_AVAILABLE, INCONCLUSIVE, UNAVAILABLE, ERROR }
