package pe.gob.reniec.credenciales.revocacion.revocation.initiation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos requeridos para iniciar una nueva solicitud y consultar si existen credenciales disponibles.")
public record StartRevocationRequest(
		@Schema(description = "Número de DNI del ciudadano. Debe contener exactamente ocho dígitos ASCII. Por privacidad, la documentación no incluye un DNI completo de ejemplo.",
				minLength = 8, maxLength = 8, pattern = DniRule.REGEX)
		@NotNull(message = "El DNI es obligatorio.")
		@Size(min = 8, max = 8, message = "El DNI debe contener 8 dígitos.")
		@Pattern(regexp = DniRule.REGEX, message = "El DNI debe contener solo dígitos.")
		String dni,
		@Schema(description = "Evidencia efímera de Google reCAPTCHA v2 Checkbox. No se almacena ni se devuelve.",
				minLength = 1, maxLength = 4096, writeOnly = true)
		@NotBlank(message = "La verificación de seguridad es obligatoria.")
		@Size(max = 4096, message = "La verificación de seguridad no tiene un formato válido.")
		String recaptchaToken) {
}
