package pe.gob.reniec.certificados.cancelacion.cancellation.reason;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReasonCode;

@Schema(description = "Motivo registrado para la solicitud activa.")
public record CancellationReasonResponse(
		String requestStatus,
		CancellationReasonCode reasonCode,
		String otherReason,
		boolean canContinue,
		String nextStep) {
}
