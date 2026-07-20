package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado normalizado de la consulta de elegibilidad.")
public enum EligibilityOutcome { ELIGIBLE, NOT_ELIGIBLE, UNAVAILABLE, INCONCLUSIVE, ERROR }
