package pe.gob.reniec.certificados.cancelacion.cancellation.confirmation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReasonCode;

@Schema(description = "Decisión completa y consentimiento explícito presentados en el paso 4.")
public record CancellationConfirmationRequest(
		@NotBlank
		@Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
		String certificateUuid,
		@NotNull
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
		CancellationReasonCode reasonCode,
		@Size(max = 300)
		@Schema(maxLength = 300, description = "Descripción requerida únicamente para OTHER.")
		String otherReason,
		@AssertTrue(message = "El consentimiento debe aceptarse expresamente")
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Aceptación expresa del texto mostrado; debe enviarse con valor true.")
		Boolean consentAccepted,
		@NotBlank @Size(max = 64)
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 64,
				description = "Versión exacta del consentimiento mostrado por el backend.")
		String consentVersion) { }
