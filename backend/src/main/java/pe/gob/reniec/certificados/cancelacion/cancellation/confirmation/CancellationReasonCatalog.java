package pe.gob.reniec.certificados.cancelacion.cancellation.confirmation;

import java.util.Map;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReasonCode;

public final class CancellationReasonCatalog {
	private static final Map<CancellationReasonCode, String> LABELS = Map.of(
			CancellationReasonCode.THEFT, "Robo",
			CancellationReasonCode.LOSS, "Pérdida",
			CancellationReasonCode.DEVICE_OR_NUMBER_CHANGE, "Cambio de equipo o número",
			CancellationReasonCode.SUSPECTED_UNAUTHORIZED_USE, "Sospecha de uso no autorizado",
			CancellationReasonCode.OTHER, "Otro motivo");

	private CancellationReasonCatalog() {
	}

	public static boolean supports(CancellationReasonCode reason) {
		return LABELS.containsKey(reason);
	}

	public static String label(CancellationReasonCode reason) {
		String label = LABELS.get(reason);
		if (label == null) {
			throw new IllegalArgumentException("Unsupported cancellation reason");
		}
		return label;
	}
}
