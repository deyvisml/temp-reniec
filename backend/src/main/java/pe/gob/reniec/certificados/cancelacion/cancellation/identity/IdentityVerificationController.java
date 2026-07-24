package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.gob.reniec.certificados.cancelacion.shared.error.ApiError;
import pe.gob.reniec.certificados.cancelacion.shared.web.CorrelationIdFilter;
import pe.gob.reniec.certificados.cancelacion.cancellation.session.FlowSessionCookieService;

@RestController
@Tag(name = "Verificación de identidad", description = "Autenticación temporal del ciudadano mediante ID Perú.")
public class IdentityVerificationController {
	private static final Logger LOGGER = LoggerFactory.getLogger(IdentityVerificationController.class);
	private final IdentityVerificationService service;
	private final FlowSessionCookieService cookies;
	private final IdentityCallbackOutcomeCookieService callbackOutcomes;
	private final IdPeruProperties properties;

	public IdentityVerificationController(IdentityVerificationService service, FlowSessionCookieService cookies,
			IdentityCallbackOutcomeCookieService callbackOutcomes, IdPeruProperties properties) {
		this.service = service;
		this.cookies = cookies;
		this.callbackOutcomes = callbackOutcomes;
		this.properties = properties;
	}

	@PostMapping(path = "/api/v1/identity-verifications", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "startIdentityVerification", summary = "Inicia la autenticación con ID Perú",
			description = "Valida la continuidad temporal, crea state y PKCE de un solo uso y devuelve la URL construida por el backend.",
			security = @SecurityRequirement(name = "FlowSessionCookie"))
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "URL de autorización preparada"),
		@ApiResponse(responseCode = "401", description = "Continuidad ausente o inválida",
				content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "409", description = "La verificación no puede iniciarse en el estado actual",
				content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "503", description = "Integración no disponible",
				content = @Content(schema = @Schema(implementation = ApiError.class))) })
	public IdentityStartResponse start(HttpServletRequest request) {
		String correlation = String.valueOf(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
		return new IdentityStartResponse(service.start(cookies.access(request).orElseThrow(() -> unauthorized()), correlation).toString());
	}

	@GetMapping(path = "/api/v1/idperu/callback")
	@Operation(operationId = "handleIdentityCallbackGet", summary = "Procesa el retorno GET de ID Perú",
			description = "Recibe el retorno del navegador, valida el intento y siempre redirige con HTTP 303 a una ruta frontend fija.")
	@ApiResponse(responseCode = "303", description = "Retorno procesado y redirección controlada")
	public ResponseEntity<Void> callbackGet(@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			@RequestParam(name = "session_state", required = false) String sessionState,
			@RequestParam(required = false) String error) {
		return handleCallback(code, state, sessionState, error);
	}

	@PostMapping(path = "/api/v1/idperu/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@Operation(operationId = "handleIdentityCallbackPost", summary = "Procesa el retorno POST de ID Perú",
			description = "Recibe el formulario del proveedor, aplica el mismo caso de uso y siempre redirige con HTTP 303 a una ruta frontend fija.")
	@ApiResponse(responseCode = "303", description = "Retorno procesado y redirección controlada")
	public ResponseEntity<Void> callbackPost(@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			@RequestParam(name = "session_state", required = false) String sessionState,
			@RequestParam(required = false) String error) {
		return handleCallback(code, state, sessionState, error);
	}

	private ResponseEntity<Void> handleCallback(String code, String state, String sessionState, String error) {
		ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.SEE_OTHER)
				.location(properties.getFrontendReturnUri());
		try {
			IdentityVerificationService.CallbackResult result = service.callback(code, state, sessionState, error);
			if (result.verified()) {
				response.header(HttpHeaders.SET_COOKIE,
						cookies.access(result.accessToken().value(), result.accessToken().expiresAt()).toString(),
						callbackOutcomes.clear().toString());
			}
			else {
				response.header(HttpHeaders.SET_COOKIE,
						callbackOutcomes.create(IdentityCallbackOutcome.fromStatus(result.status())).toString());
			}
		}
		catch (IdentityIntegrationException exception) {
			response.header(HttpHeaders.SET_COOKIE,
					callbackOutcomes.create(IdentityCallbackOutcome.fromFailure(exception.failure())).toString());
		}
		catch (RuntimeException exception) {
			LOGGER.error("Unexpected ID Peru callback failure type={}", exception.getClass().getSimpleName());
			response.header(HttpHeaders.SET_COOKIE,
					callbackOutcomes.create(IdentityCallbackOutcome.ERROR).toString());
		}
		return response.build();
	}

	@GetMapping(path = "/api/v1/identity-verifications/current", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getCurrentIdentityVerification", summary = "Consulta el estado de autenticación actual",
			description = "Resuelve el intento desde la cookie HttpOnly, valida la autorización y consume el resultado de presentación del callback.",
			security = @SecurityRequirement(name = "FlowSessionCookie"))
	public ResponseEntity<CurrentIdentityResponse> current(HttpServletRequest request) {
		IdentityVerificationService.CurrentIdentityStatus status = service.current(cookies.access(request).orElseThrow(() -> unauthorized()));
		IdentityCallbackOutcome callbackOutcome = callbackOutcomes.read(request).orElse(null);
		ResponseEntity.BodyBuilder response = ResponseEntity.ok();
		if (callbackOutcome != null) {
			response.header(HttpHeaders.SET_COOKIE, callbackOutcomes.clear().toString());
		}
		return response.body(new CurrentIdentityResponse(status.status(), status.canContinue(), status.nextStep(),
				callbackOutcome == null ? null : callbackOutcome.name()));
	}

	private static pe.gob.reniec.certificados.cancelacion.cancellation.session.FlowSessionException unauthorized() {
		return new pe.gob.reniec.certificados.cancelacion.cancellation.session.FlowSessionException(
				pe.gob.reniec.certificados.cancelacion.cancellation.session.FlowSessionException.Reason.REQUIRED);
	}

	public record IdentityStartResponse(
			@Schema(description = "URL de autorización construida por el backend.", requiredMode = Schema.RequiredMode.REQUIRED,
					format = "uri")
			String authorizationUrl) { }

	public record CurrentIdentityResponse(
			@Schema(description = "Estado normalizado del intento de identidad.", requiredMode = Schema.RequiredMode.REQUIRED,
					allowableValues = { "STARTED", "VERIFIED", "REJECTED", "CANCELLED", "EXPIRED",
							"IDENTITY_MISMATCH", "ERROR" })
			String status,
			@Schema(description = "Indica si la autorización temporal permite continuar.",
					requiredMode = Schema.RequiredMode.REQUIRED)
			boolean canContinue,
			@Schema(description = "Siguiente paso autorizado.", requiredMode = Schema.RequiredMode.REQUIRED,
					allowableValues = { "IDENTITY_VERIFICATION", "CERTIFICATE_SELECTION" })
			String nextStep,
			@Schema(description = "Resultado efímero del último callback, consumido una sola vez para presentación.",
					nullable = true, allowableValues = { "CANCELLED", "REJECTED", "IDENTITY_MISMATCH",
							"EXPIRED", "TIMEOUT", "UNAVAILABLE", "ERROR" })
			String callbackOutcome) { }
}
