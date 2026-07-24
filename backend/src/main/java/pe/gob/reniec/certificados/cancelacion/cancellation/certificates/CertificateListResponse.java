package pe.gob.reniec.certificados.cancelacion.cancellation.certificates;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Listado persistido de certificados vigentes de la solicitud autenticada.")
public record CertificateListResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String requestStatus,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CertificateItem> certificates,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) int selectedCount,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean canContinue) {

	@Schema(description = "Certificado vigente obtenido después de autenticar al ciudadano.")
	public record CertificateItem(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 64) String orderNumber,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "date-time") Instant emissionCreatedAt,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") String certificateUuid,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String availabilityStatus,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean selected) { }
}
