package pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Listado persistido de credenciales digitales de la solicitud autenticada.")
public record DigitalCredentialListResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String requestStatus,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<DigitalCredentialItem> digitalCredentials,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean canContinue) {

	@Schema(description = "Credencial digital obtenida después de autenticar al ciudadano.")
	public record DigitalCredentialItem(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0") int statusListIndex,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant emissionCreatedAt,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") String digitalCredentialUuid,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) DigitalCredentialStatus status,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time", nullable = true) Instant revokedAt,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean selected) { }
}
