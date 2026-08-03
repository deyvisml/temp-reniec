package pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials;

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

import pe.gob.reniec.credenciales.revocacion.revocation.session.FlowSessionCookieService;
import pe.gob.reniec.credenciales.revocacion.revocation.session.FlowSessionException;
import pe.gob.reniec.credenciales.revocacion.revocation.session.FlowSessionService;
import pe.gob.reniec.credenciales.revocacion.shared.error.ApiError;
import pe.gob.reniec.credenciales.revocacion.shared.web.CorrelationIdFilter;

@RestController
@RequestMapping("/api/v1/revocation-requests/current")
@Tag(name = "Credenciales vigentes",
		description = "Listado autenticado de credenciales de la operación activa.")
@SecurityRequirement(name = "FlowSessionCookie")
public class DigitalCredentialListingController {

	private final DigitalCredentialListingService service;
	private final FlowSessionService sessions;
	private final FlowSessionCookieService cookies;

	public DigitalCredentialListingController(DigitalCredentialListingService service, FlowSessionService sessions,
			FlowSessionCookieService cookies) {
		this.service = service;
		this.sessions = sessions;
		this.cookies = cookies;
	}

	@GetMapping(value = "/digital-credentials", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getCurrentRequestDigitalCredentials",
			summary = "Obtiene el listado de credenciales de la solicitud autenticada",
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
	public DigitalCredentialListResponse list(HttpServletRequest request) {
		return service.list(requestId(request), correlation(request));
	}

	private Long requestId(HttpServletRequest request) {
		String token = cookies.access(request).orElseThrow(() ->
				new FlowSessionException(FlowSessionException.Reason.REQUIRED));
		FlowSessionService.CurrentSession current = sessions.current(token);
		if (!"IDENTITY_VERIFIED".equals(current.sessionStatus())) {
			throw new DigitalCredentialListingException(DigitalCredentialListingException.Reason.IDENTITY_REQUIRED,
					"Identity verification is required");
		}
		return current.requestId();
	}

	private static String correlation(HttpServletRequest request) {
		return String.valueOf(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
	}
}
