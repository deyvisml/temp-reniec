package pe.gob.reniec.certificados.cancelacion.cancellation.session;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/session")
@Tag(name = "Sesión del flujo", description = "Continuidad transaccional de la operación ciudadana activa.")
public class FlowSessionController {
	private final FlowSessionService service;
	private final FlowSessionCookieService cookies;
	public FlowSessionController(FlowSessionService service, FlowSessionCookieService cookies) {
		this.service = service; this.cookies = cookies;
	}
	@GetMapping("/current")
	@Operation(operationId = "getCurrentFlowSession", summary = "Consulta la sesión y el paso actualmente autorizado",
			security = @SecurityRequirement(name = "FlowSessionCookie"))
	public ResponseEntity<FlowSessionService.CurrentSession> current(HttpServletRequest request) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(service.current(cookies.access(request).orElseThrow(() -> required())));
	}
	@PostMapping("/refresh")
	@Operation(operationId = "refreshFlowSession", summary = "Rota el refresh token y renueva el access token",
			security = @SecurityRequirement(name = "FlowRefreshCookie"))
	public ResponseEntity<Void> refresh(HttpServletRequest request) {
		FlowSessionService.Tokens tokens = service.refresh(cookies.refresh(request).orElseThrow(() -> required()));
		return ResponseEntity.noContent().headers(cookies.headers(tokens)).build();
	}
	@PostMapping("/logout")
	@Operation(operationId = "logoutFlowSession", summary = "Cierra la sesión y abandona la operación activa reversible",
			security = @SecurityRequirement(name = "FlowSessionCookie"))
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		cookies.access(request).ifPresent(service::logout);
		return ResponseEntity.noContent().headers(cookies.clearHeaders()).build();
	}
	private static FlowSessionException required() { return new FlowSessionException(FlowSessionException.Reason.REQUIRED); }
}
