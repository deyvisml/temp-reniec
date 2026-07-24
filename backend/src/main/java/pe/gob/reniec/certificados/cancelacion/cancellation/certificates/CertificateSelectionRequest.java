package pe.gob.reniec.certificados.cancelacion.cancellation.certificates;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Conjunto completo de certificados que el ciudadano selecciona para cancelar.")
public record CertificateSelectionRequest(
		@NotEmpty
		@Size(max = 100)
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 100,
				description = "UUID canónicos de la solicitud activa; no admite duplicados.")
		List<@NotBlank @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$") String> certificateUuids) { }
