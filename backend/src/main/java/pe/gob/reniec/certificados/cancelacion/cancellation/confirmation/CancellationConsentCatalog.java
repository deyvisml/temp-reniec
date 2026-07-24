package pe.gob.reniec.certificados.cancelacion.cancellation.confirmation;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public final class CancellationConsentCatalog {

	public static final String VERSION = "CANCELACION_CERTIFICADOS_V1";

	private static final String TEXT = "Confirmo que revisé los certificados seleccionados y comprendo "
			+ "que su cancelación será inmediata. Una vez cancelados, dejarán de ser válidos.";

	private static final List<String> CONSEQUENCES = List.of(
			"La cancelación se ejecutará de forma inmediata en el siguiente paso.",
			"Los certificados seleccionados dejarán de ser válidos.",
			"Solo se procesará el conjunto de certificados que seleccionaste.");

	public String version() { return VERSION; }
	public String text() { return TEXT; }
	public List<String> consequences() { return CONSEQUENCES; }
}
