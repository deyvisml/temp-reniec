package pe.gob.reniec.certificados.cancelacion.cancellation.confirmation;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public final class CancellationConsentCatalog {

	public static final String VERSION = "CANCELACION_CERTIFICADOS_V1";

	private static final String TEXT = "Confirmo que revisé el certificado seleccionado y comprendo "
			+ "que su cancelación será inmediata. Una vez cancelado, dejará de ser válido.";

	private static final List<String> CONSEQUENCES = List.of(
			"La cancelación se ejecutará de forma inmediata en el siguiente paso.",
			"El certificado seleccionado dejará de ser válido.",
			"Solo se procesará el certificado que seleccionaste.");

	public String version() { return VERSION; }
	public String text() { return TEXT; }
	public List<String> consequences() { return CONSEQUENCES; }
}
