package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StartCancellationRequest(
		@NotNull(message = "El DNI es obligatorio.")
		@Size(min = 8, max = 8, message = "El DNI debe contener 8 dígitos.")
		@Pattern(regexp = DniRule.REGEX, message = "El DNI debe contener solo dígitos.")
		String dni) {
}
