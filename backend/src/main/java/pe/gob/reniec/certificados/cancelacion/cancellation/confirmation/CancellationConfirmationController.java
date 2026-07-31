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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pe.gob.reniec.certificados.cancelacion.cancellation.execution.CancellationExecutionResponse;
import pe.gob.reniec.certificados.cancelacion.cancellation.execution.CancellationExecutionService;
import pe.gob.reniec.certificados.cancelacion.cancellation.session.*;
import pe.gob.reniec.certificados.cancelacion.shared.error.ApiError;
import pe.gob.reniec.certificados.cancelacion.shared.web.CorrelationIdFilter;

@RestController
@RequestMapping("/api/v1/cancellation-requests/current")
@Tag(name = "Confirmación de cancelación",
		description = "Revisión efímera y confirmación atómica de la operación autenticada.")
@SecurityRequirement(name = "FlowSessionCookie")
public class CancellationConfirmationController {

	private final CancellationConfirmationService service;
	private final FlowSessionService sessions;
	private final FlowSessionCookieService cookies;
	private final CancellationExecutionService execution;

	public CancellationConfirmationController(CancellationConfirmationService service,
			FlowSessionService sessions, FlowSessionCookieService cookies,
			ObjectProvider<CancellationExecutionService> execution) {
		this.service = service;
		this.sessions = sessions;
		this.cookies = cookies;
		this.execution = execution.getIfAvailable();
	}

	@GetMapping(value = "/review", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getConfirmedCancellationReview",
			summary = "Recupera el resumen de una solicitud confirmada",
			description = "Disponible únicamente después de confirmar; no recupera borradores.")
	public CancellationReviewResponse confirmed(HttpServletRequest request) {
		return service.confirmed(requestId(request));
	}

	@PostMapping(value = "/review", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "previewCurrentCancellation",
			summary = "Valida el borrador y prepara el resumen sin persistirlo")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Resumen vigente sin persistencia del borrador"),
		@ApiResponse(responseCode = "400", description = "Formato inválido", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "401", description = "Sesión ausente o expirada", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "403", description = "Identidad o paso no permitido", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "422", description = "Motivo o certificado inválido", content = @Content(schema = @Schema(implementation = ApiError.class)))
	})
	public CancellationReviewResponse preview(@Valid @RequestBody CancellationReviewRequest body,
			HttpServletRequest request) {
		return service.preview(requestId(request), body);
	}

	@PostMapping(value = "/confirmation", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "confirmCurrentCancellation",
			summary = "Persiste la decisión completa y confirma la solicitud",
			description = "Guarda la decisión, ejecuta una revocación idempotente y genera la constancia cuando el resultado es exitoso.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Solicitud confirmada o repetición idempotente"),
		@ApiResponse(responseCode = "400", description = "Consentimiento ausente o formato inválido", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "401", description = "Sesión ausente o expirada", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "403", description = "Identidad o paso no permitido", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "409", description = "Versión o decisión incompatible", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "422", description = "Motivo o selección inválida", content = @Content(schema = @Schema(implementation = ApiError.class))),
		@ApiResponse(responseCode = "503", description = "Integración de revocación no disponible; la decisión no se persiste", content = @Content(schema = @Schema(implementation = ApiError.class)))
	})
	public CancellationExecutionResponse confirm(@Valid @RequestBody CancellationConfirmationRequest body,
			HttpServletRequest request) {
		return execution().confirmAndExecute(requestId(request), body, correlation(request));
	}

	@GetMapping(value = "/outcome", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "getCurrentCancellationOutcome",
			summary = "Consulta el resultado y la constancia de la solicitud actual")
	public CancellationExecutionResponse outcome(HttpServletRequest request) {
		return execution().current(requestId(request));
	}

	@PostMapping(value = "/execution", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "resumeCurrentCancellationExecution",
			summary = "Reanuda idempotentemente una revocación ya confirmada")
	public CancellationExecutionResponse resume(HttpServletRequest request) {
		return execution().execute(requestId(request), correlation(request));
	}

	@PostMapping(value = "/receipt/retry", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "retryCurrentCancellationReceipt",
			summary = "Reintenta únicamente la generación de la constancia")
	public CancellationExecutionResponse retryReceipt(HttpServletRequest request) {
		return execution().retryReceipt(requestId(request), correlation(request));
	}

	@GetMapping(value = "/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
	@Operation(operationId = "downloadCurrentCancellationReceipt",
			summary = "Descarga la constancia de la sesión autenticada")
	public ResponseEntity<byte[]> receipt(HttpServletRequest request) {
		byte[] document = execution().receiptDocument(requestId(request));
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"constancia-cancelacion.pdf\"")
				.contentType(MediaType.APPLICATION_PDF)
				.body(document);
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

	private CancellationExecutionService execution() {
		if (execution != null) return execution;
		throw new CancellationConfirmationException(
				CancellationConfirmationException.Reason.NOT_ALLOWED,
				"Cancellation execution is unavailable");
	}

	private static String correlation(HttpServletRequest request) {
		return String.valueOf(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
	}
}
