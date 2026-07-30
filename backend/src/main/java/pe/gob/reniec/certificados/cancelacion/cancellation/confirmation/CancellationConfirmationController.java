package pe.gob.reniec.certificados.cancelacion.cancellation.confirmation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import pe.gob.reniec.certificados.cancelacion.cancellation.session.*;
import pe.gob.reniec.certificados.cancelacion.shared.error.ApiError;
import pe.gob.reniec.certificados.cancelacion.shared.web.CorrelationIdFilter;

@RestController
@RequestMapping("/api/v1/cancellation-requests/current")
@Tag(name = "Confirmación de cancelación", description = "Revisión y consentimiento de la operación autenticada.")
@SecurityRequirement(name = "FlowSessionCookie")
public class CancellationConfirmationController {

	private final CancellationConfirmationService service;
	private final FlowSessionService sessions;
	private final FlowSessionCookieService cookies;

	public CancellationConfirmationController(CancellationConfirmationService service,
			FlowSessionService sessions, FlowSessionCookieService cookies) {
		this.service = service;
		this.sessions = sessions;
		this.cookies = cookies;
	}

	@GetMapping(value = "/review", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getCurrentCancellationReview",
			summary = "Obtiene el resumen autoritativo previo a confirmar",
			description = "Construye el paso 4 desde persistencia con exactamente un certificado y devuelve solo identificadores enmascarados.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Resumen vigente o confirmación ya registrada"),
		@ApiResponse(responseCode = "401", description = "Sesión ausente o expirada", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "403", description = "Identidad o paso no permitido", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "409", description = "Solicitud en un estado incompatible", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "422", description = "Motivo o selección persistida inválida", content = @Content(schema = @Schema(implementation = ApiError.class)))
	})
	public CancellationReviewResponse review(HttpServletRequest request) {
		return service.review(requestId(request));
	}

	@PostMapping(value = "/confirmation", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "confirmCurrentCancellation",
			summary = "Registra el consentimiento y confirma la solicitud",
			description = "Confirma de forma idempotente sin ejecutar todavía la revocación.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Solicitud confirmada o repetición idempotente"),
		@ApiResponse(responseCode = "400", description = "Consentimiento ausente o formato inválido", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "401", description = "Sesión ausente o expirada", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "403", description = "Identidad o paso no permitido", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "409", description = "Versión cambiada, estado incompatible o concurrencia", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "422", description = "Motivo o selección persistida inválida", content = @Content(schema = @Schema(implementation = ApiError.class)))
	})
	public CancellationReviewResponse confirm(@Valid @RequestBody CancellationConfirmationRequest body,
			HttpServletRequest request) {
		return service.confirm(requestId(request), body, correlation(request));
	}

	private Long requestId(HttpServletRequest request) {
		String token = cookies.access(request).orElseThrow(() ->
				new FlowSessionException(FlowSessionException.Reason.REQUIRED));
		FlowSessionService.CurrentSession current = sessions.current(token);
		if (!"IDENTITY_VERIFIED".equals(current.sessionStatus())) {
			throw new CancellationConfirmationException(
					CancellationConfirmationException.Reason.IDENTITY_REQUIRED,
					"Identity verification is required");
		}
		return current.requestId();
	}

	private static String correlation(HttpServletRequest request) {
		return String.valueOf(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
	}
}
