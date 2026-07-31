package pe.gob.reniec.certificados.cancelacion.cancellation.certificates;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.gob.reniec.certificados.cancelacion.cancellation.session.FlowSessionCookieService;
import pe.gob.reniec.certificados.cancelacion.cancellation.session.FlowSessionException;
import pe.gob.reniec.certificados.cancelacion.cancellation.session.FlowSessionService;
import pe.gob.reniec.certificados.cancelacion.shared.error.ApiError;
import pe.gob.reniec.certificados.cancelacion.shared.web.CorrelationIdFilter;

@RestController
@RequestMapping("/api/v1/cancellation-requests/current")
@Tag(name = "Certificados vigentes",
		description = "Listado autenticado de certificados de la operación activa.")
@SecurityRequirement(name = "FlowSessionCookie")
public class CertificateListingController {

	private final CertificateListingService service;
	private final FlowSessionService sessions;
	private final FlowSessionCookieService cookies;

	public CertificateListingController(CertificateListingService service, FlowSessionService sessions,
			FlowSessionCookieService cookies) {
		this.service = service;
		this.sessions = sessions;
		this.cookies = cookies;
	}

	@GetMapping(value = "/certificates", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getCurrentRequestCertificates",
			summary = "Obtiene el listado de certificados de la solicitud autenticada",
			description = "Consulta el segundo servicio solo en la primera carga y luego devuelve la instantánea persistida.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Listado persistido, incluido el escenario vacío"),
		@ApiResponse(responseCode = "401", description = "Sesión ausente o expirada", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "403", description = "Identidad no verificada o paso no permitido", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "409", description = "Consulta concurrente", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "422", description = "Respuesta externa inválida", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "503", description = "Servicio de listado no disponible", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "504", description = "Timeout del servicio de listado", content = @Content(schema = @Schema(implementation = ApiError.class)))
	})
	public CertificateListResponse list(HttpServletRequest request) {
		return service.list(requestId(request), correlation(request));
	}

	private Long requestId(HttpServletRequest request) {
		String token = cookies.access(request).orElseThrow(() ->
				new FlowSessionException(FlowSessionException.Reason.REQUIRED));
		FlowSessionService.CurrentSession current = sessions.current(token);
		if (!"IDENTITY_VERIFIED".equals(current.sessionStatus())) {
			throw new CertificateListingException(CertificateListingException.Reason.IDENTITY_REQUIRED,
					"Identity verification is required");
		}
		return current.requestId();
	}

	private static String correlation(HttpServletRequest request) {
		return String.valueOf(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
	}
}
