package pe.gob.reniec.certificados.cancelacion.cancellation.certificates;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Certificado que el ciudadano selecciona para cancelar.")
public record CertificateSelectionRequest(
		@NotBlank
		@Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid",
				description = "UUID canónico de un certificado disponible de la solicitud activa.")
		String certificateUuid) { }
