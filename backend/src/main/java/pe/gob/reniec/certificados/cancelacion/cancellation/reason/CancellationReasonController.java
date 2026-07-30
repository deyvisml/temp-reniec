package pe.gob.reniec.certificados.cancelacion.cancellation.reason;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import pe.gob.reniec.certificados.cancelacion.cancellation.session.*;
import pe.gob.reniec.certificados.cancelacion.shared.web.CorrelationIdFilter;

@RestController
@RequestMapping("/api/v1/cancellation-requests/current/reason")
@Tag(name = "Motivo de cancelación", description = "Registro del motivo de la solicitud autenticada.")
@SecurityRequirement(name = "FlowSessionCookie")
public class CancellationReasonController {
	private final CancellationReasonService service;
	private final FlowSessionService sessions;
	private final FlowSessionCookieService cookies;

	public CancellationReasonController(CancellationReasonService service, FlowSessionService sessions,
			FlowSessionCookieService cookies) {
		this.service = service;
		this.sessions = sessions;
		this.cookies = cookies;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getCurrentCancellationReason", summary = "Obtiene el motivo de la solicitud activa")
	public CancellationReasonResponse current(HttpServletRequest request) {
		return service.current(requestId(request));
	}

	@PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "replaceCurrentCancellationReason", summary = "Registra o reemplaza el motivo antes de la confirmación")
	public CancellationReasonResponse register(@Valid @RequestBody CancellationReasonRequest body,
			HttpServletRequest request) {
		return service.register(requestId(request), body,
				String.valueOf(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE)));
	}

	private Long requestId(HttpServletRequest request) {
		String token = cookies.access(request).orElseThrow(() ->
				new FlowSessionException(FlowSessionException.Reason.REQUIRED));
		FlowSessionService.CurrentSession current = sessions.current(token);
		if (!"IDENTITY_VERIFIED".equals(current.sessionStatus())) {
			throw new CancellationReasonException(CancellationReasonException.Reason.IDENTITY_REQUIRED,
					"Identity verification is required");
		}
		return current.requestId();
	}
}
