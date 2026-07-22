package pe.gob.reniec.certificados.cancelacion.cancellation.initiation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.gob.reniec.certificados.cancelacion.shared.error.ApiError;
import pe.gob.reniec.certificados.cancelacion.shared.web.CorrelationIdFilter;

@RestController
@RequestMapping("/api/v1/cancellation-requests")
@Tag(name = "Solicitudes de cancelación", description = "Inicio de solicitudes y consulta de existencia de certificados.")
public class CancellationRequestController {

	private final CancellationRequestInitiationService service;

	public CancellationRequestController(CancellationRequestInitiationService service) {
		this.service = service;
	}

	@Operation(operationId = "initiateCancellationRequest",
			summary = "Inicia una solicitud y consulta si existen certificados disponibles",
			description = "Valida el DNI y Google reCAPTCHA v2 Checkbox antes de crear una solicitud y consultar únicamente si existe al menos un certificado disponible para cancelar. No obtiene una lista, cantidad, número de orden, fecha de creación ni UUID; tampoco reabre solicitudes anteriores ni expone el DNI completo o la evidencia anti-bot.",
			parameters = @Parameter(name = CorrelationIdFilter.HEADER_NAME, in = ParameterIn.HEADER,
					description = "Identificador opcional de correlación. Debe tener entre 1 y 64 caracteres ASCII válidos.",
					required = false, schema = @Schema(type = "string", maxLength = 64,
							pattern = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}")))
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Resultado normalizado de existencia de certificados",
					headers = @Header(name = CorrelationIdFilter.HEADER_NAME, description = "Identificador de correlación",
							schema = @Schema(type = "string")),
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = CancellationRequestResponse.class))),
			@ApiResponse(responseCode = "400", description = "DNI, JSON, cuerpo o evidencia reCAPTCHA inválida: VALIDATION_ERROR, RECAPTCHA_REQUIRED, RECAPTCHA_REJECTED o RECAPTCHA_EXPIRED_OR_DUPLICATE", headers = @Header(name = CorrelationIdFilter.HEADER_NAME, schema = @Schema(type = "string")), content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "409", description = "Conflicto controlado: AVAILABILITY_CHECK_IN_PROGRESS, CANCELLATION_REQUEST_IN_PROGRESS o CONCURRENT_REQUEST", headers = @Header(name = CorrelationIdFilter.HEADER_NAME, schema = @Schema(type = "string")), content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "415", description = "Tipo de contenido no admitido", headers = @Header(name = CorrelationIdFilter.HEADER_NAME, schema = @Schema(type = "string")), content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Error interno controlado", headers = @Header(name = CorrelationIdFilter.HEADER_NAME, schema = @Schema(type = "string")), content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "502", description = "RECAPTCHA_INVALID_RESPONSE o error controlado del proveedor de disponibilidad", headers = @Header(name = CorrelationIdFilter.HEADER_NAME, schema = @Schema(type = "string")), content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "503", description = "RECAPTCHA_UNAVAILABLE o servicio de disponibilidad no disponible", headers = @Header(name = CorrelationIdFilter.HEADER_NAME, schema = @Schema(type = "string")), content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "504", description = "RECAPTCHA_TIMEOUT o tiempo de espera agotado en disponibilidad", headers = @Header(name = CorrelationIdFilter.HEADER_NAME, schema = @Schema(type = "string")), content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class)))
	})
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<CancellationRequestResponse> initiate(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
				description = "DNI y evidencia efímera reCAPTCHA que se validan en el backend antes de crear una solicitud.",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = StartCancellationRequest.class)))
			@Valid @RequestBody StartCancellationRequest body,
			@Parameter(hidden = true) HttpServletRequest request) {
		String correlationId = String.valueOf(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
		return ResponseEntity.ok(service.initiate(body.dni(), body.recaptchaToken(), correlationId));
	}
}
