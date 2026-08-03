package pe.gob.reniec.credenciales.revocacion.revocation.confirmation;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public final class RevocationConsentCatalog {

	public static final String VERSION = "REVOCACION_CREDENCIALES_DIGITALES_V1";

	private static final String TEXT = "Confirmo que revisé la credencial seleccionada y comprendo "
			+ "que su revocación será inmediata. Una vez revocado, dejará de ser válido.";

	private static final List<String> CONSEQUENCES = List.of(
			"La revocación se ejecutará de forma inmediata en el siguiente paso.",
			"La credencial seleccionada dejará de ser válido.",
			"Solo se procesará la credencial que seleccionaste.");

	public String version() { return VERSION; }
	public String text() { return TEXT; }
	public List<String> consequences() { return CONSEQUENCES; }
}
