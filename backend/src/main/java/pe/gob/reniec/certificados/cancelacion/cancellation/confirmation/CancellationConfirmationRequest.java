package pe.gob.reniec.certificados.cancelacion.cancellation.confirmation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Evidencia explícita del consentimiento mostrado en el paso 4.")
public record CancellationConfirmationRequest(
		@AssertTrue(message = "El consentimiento debe aceptarse expresamente")
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Aceptación expresa del texto mostrado; debe enviarse con valor true.")
		Boolean consentAccepted,
		@NotBlank @Size(max = 64)
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 64,
				description = "Versión exacta del consentimiento mostrado por el backend.")
		String consentVersion) { }
