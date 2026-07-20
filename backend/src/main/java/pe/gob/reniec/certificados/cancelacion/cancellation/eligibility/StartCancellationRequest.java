package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos requeridos para iniciar o recuperar una solicitud y consultar su elegibilidad.")
public record StartCancellationRequest(
		@Schema(description = "Número de DNI del ciudadano. Debe contener exactamente ocho dígitos ASCII. Por privacidad, la documentación no incluye un DNI completo de ejemplo.",
				minLength = 8, maxLength = 8, pattern = DniRule.REGEX)
		@NotNull(message = "El DNI es obligatorio.")
		@Size(min = 8, max = 8, message = "El DNI debe contener 8 dígitos.")
		@Pattern(regexp = DniRule.REGEX, message = "El DNI debe contener solo dígitos.")
		String dni) {
}
