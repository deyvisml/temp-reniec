package pe.gob.reniec.credenciales.revocacion.revocation.confirmation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReasonCode;

@Schema(description = "Borrador efímero que se valida para presentar el paso 4 sin persistirlo.")
public record RevocationReviewRequest(
		@NotBlank
		@Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
		String digitalCredentialUuid,
		@NotNull
		@PositiveOrZero
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
		Integer statusListIndex,
		@NotNull
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
		RevocationReasonCode reasonCode,
		@Size(max = 300)
		@Schema(maxLength = 300, description = "Descripción requerida únicamente para OTHER.")
		String otherReason) { }
