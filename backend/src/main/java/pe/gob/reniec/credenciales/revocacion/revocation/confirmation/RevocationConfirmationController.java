package pe.gob.reniec.credenciales.revocacion.revocation.confirmation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pe.gob.reniec.credenciales.revocacion.revocation.execution.RevocationExecutionResponse;
import pe.gob.reniec.credenciales.revocacion.revocation.execution.RevocationExecutionService;
import pe.gob.reniec.credenciales.revocacion.revocation.session.*;
import pe.gob.reniec.credenciales.revocacion.shared.error.ApiError;
import pe.gob.reniec.credenciales.revocacion.shared.web.CorrelationIdFilter;

@RestController
@RequestMapping("/api/v1/revocation-requests/current")
@Tag(name = "Confirmación de revocación",
		description = "Revisión efímera y confirmación atómica de la operación autenticada.")
@SecurityRequirement(name = "FlowSessionCookie")
public class RevocationConfirmationController {

	private final RevocationConfirmationService service;
	private final FlowSessionService sessions;
	private final FlowSessionCookieService cookies;
	private final RevocationExecutionService execution;

	public RevocationConfirmationController(RevocationConfirmationService service,
			FlowSessionService sessions, FlowSessionCookieService cookies,
			ObjectProvider<RevocationExecutionService> execution) {
		this.service = service;
		this.sessions = sessions;
		this.cookies = cookies;
		this.execution = execution.getIfAvailable();
	}

	@GetMapping(value = "/review", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getConfirmedRevocationReview",
			summary = "Recupera el resumen de una solicitud confirmada",
			description = "Disponible únicamente después de confirmar; no recupera borradores.")
	public RevocationReviewResponse confirmed(HttpServletRequest request) {
		return service.confirmed(requestId(request));
	}

	@PostMapping(value = "/review", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "previewCurrentRevocation",
			summary = "Valida el borrador y prepara el resumen sin persistirlo")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Resumen vigente sin persistencia del borrador"),
		@ApiResponse(responseCode = "400", description = "Formato inválido", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "401", description = "Sesión ausente o expirada", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "403", description = "Identidad o paso no permitido", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "422", description = "Motivo o credencial inválido", content = @Content(schema = @Schema(implementation = ApiError.class)))
	})
	public RevocationReviewResponse preview(@Valid @RequestBody RevocationReviewRequest body,
			HttpServletRequest request) {
		return service.preview(requestId(request), body);
	}

	@PostMapping(value = "/confirmation", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "confirmCurrentRevocation",
			summary = "Persiste la decisión completa y confirma la solicitud",
			description = "Guarda la decisión, ejecuta una revocación idempotente y genera la constancia cuando el resultado es exitoso.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Solicitud confirmada o repetición idempotente"),
		@ApiResponse(responseCode = "400", description = "Consentimiento ausente o formato inválido", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "401", description = "Sesión ausente o expirada", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "403", description = "Identidad o paso no permitido", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "409", description = "Versión, decisión o vigencia incompatible", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "422", description = "Motivo, selección o respuesta del proveedor inválida", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "503", description = "Proveedor de listado o revocación no disponible; la decisión no se persiste", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "504", description = "Timeout al revalidar la credencial; la decisión no se persiste", content = @Content(schema = @Schema(implementation = ApiError.class)))
	})
	public RevocationExecutionResponse confirm(@Valid @RequestBody RevocationConfirmationRequest body,
			HttpServletRequest request) {
		return execution().confirmAndExecute(requestId(request), body, correlation(request));
	}

	@GetMapping(value = "/outcome", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getCurrentRevocationOutcome",
			summary = "Consulta el resultado y la constancia de la solicitud actual")
	public RevocationExecutionResponse outcome(HttpServletRequest request) {
		return execution().current(requestId(request));
	}

	@PostMapping(value = "/execution", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "resumeCurrentRevocationExecution",
			summary = "Reanuda idempotentemente una revocación ya confirmada")
	public RevocationExecutionResponse resume(HttpServletRequest request) {
		return execution().execute(requestId(request), correlation(request));
	}

	@PostMapping(value = "/receipt/retry", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "retryCurrentRevocationReceipt",
			summary = "Reintenta únicamente la generación de la constancia")
	public RevocationExecutionResponse retryReceipt(HttpServletRequest request) {
		return execution().retryReceipt(requestId(request), correlation(request));
	}

	@GetMapping(value = "/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
	@Operation(operationId = "downloadCurrentRevocationReceipt",
			summary = "Descarga la constancia de la sesión autenticada")
	public ResponseEntity<byte[]> receipt(HttpServletRequest request) {
		byte[] document = execution().receiptDocument(requestId(request));
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"constancia-revocacion.pdf\"")
				.contentType(MediaType.APPLICATION_PDF)
				.body(document);
	}

	private Long requestId(HttpServletRequest request) {
		String token = cookies.access(request).orElseThrow(() ->
				new FlowSessionException(FlowSessionException.Reason.REQUIRED));
		FlowSessionService.CurrentSession current = sessions.current(token);
		if (!"IDENTITY_VERIFIED".equals(current.sessionStatus())) {
			throw new RevocationConfirmationException(
					RevocationConfirmationException.Reason.IDENTITY_REQUIRED,
					"Identity verification is required");
		}
		return current.requestId();
	}

	private RevocationExecutionService execution() {
		if (execution != null) return execution;
		throw new RevocationConfirmationException(
				RevocationConfirmationException.Reason.NOT_ALLOWED,
				"Revocation execution is unavailable");
	}

	private static String correlation(HttpServletRequest request) {
		return String.valueOf(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
	}
}
