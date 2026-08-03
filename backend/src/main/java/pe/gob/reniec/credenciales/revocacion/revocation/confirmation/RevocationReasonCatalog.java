package pe.gob.reniec.credenciales.revocacion.revocation.confirmation;

import java.util.Map;

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReasonCode;

public final class RevocationReasonCatalog {
	private static final Map<RevocationReasonCode, String> LABELS = Map.of(
			RevocationReasonCode.THEFT, "Robo",
			RevocationReasonCode.LOSS, "Pérdida",
			RevocationReasonCode.DEVICE_OR_NUMBER_CHANGE, "Cambio de equipo o número",
			RevocationReasonCode.SUSPECTED_UNAUTHORIZED_USE, "Sospecha de uso no autorizado",
			RevocationReasonCode.OTHER, "Otro motivo");

	private RevocationReasonCatalog() {
	}

	public static boolean supports(RevocationReasonCode reason) {
		return LABELS.containsKey(reason);
	}

	public static String label(RevocationReasonCode reason) {
		String label = LABELS.get(reason);
		if (label == null) {
			throw new IllegalArgumentException("Unsupported revocation reason");
		}
		return label;
	}
}
