package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

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
@Tag(name = "Solicitudes de cancelación", description = "Inicio, recuperación y elegibilidad del flujo ciudadano.")
public class CancellationRequestController {

	private final EligibilityInitiationService service;

	public CancellationRequestController(EligibilityInitiationService service) {
		this.service = service;
	}

	@Operation(operationId = "initiateCancellationRequest",
			summary = "Inicia o recupera una solicitud y consulta su elegibilidad",
			description = "Valida el DNI, recupera una solicitud compatible cuando existe o crea una nueva, y consulta si hay certificados digitales susceptibles de cancelación. No devuelve certificados individuales ni expone el DNI completo.",
			parameters = @Parameter(name = CorrelationIdFilter.HEADER_NAME, in = ParameterIn.HEADER,
					description = "Identificador opcional de correlación. Debe tener entre 1 y 64 caracteres ASCII válidos.",
					required = false, schema = @Schema(type = "string", maxLength = 64,
							pattern = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}")))
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Resultado normalizado de elegibilidad",
					headers = @Header(name = CorrelationIdFilter.HEADER_NAME, description = "Identificador de correlación",
							schema = @Schema(type = "string")),
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = CancellationRequestResponse.class))),
			@ApiResponse(responseCode = "400", description = "DNI, JSON o cuerpo inválido", headers = @Header(name = CorrelationIdFilter.HEADER_NAME, schema = @Schema(type = "string")), content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "409", description = "Consulta en curso o conflicto concurrente", headers = @Header(name = CorrelationIdFilter.HEADER_NAME, schema = @Schema(type = "string")), content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "415", description = "Tipo de contenido no admitido", headers = @Header(name = CorrelationIdFilter.HEADER_NAME, schema = @Schema(type = "string")), content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Error interno controlado", headers = @Header(name = CorrelationIdFilter.HEADER_NAME, schema = @Schema(type = "string")), content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "502", description = "Error controlado del proveedor de elegibilidad", headers = @Header(name = CorrelationIdFilter.HEADER_NAME, schema = @Schema(type = "string")), content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "503", description = "Servicio de elegibilidad no disponible", headers = @Header(name = CorrelationIdFilter.HEADER_NAME, schema = @Schema(type = "string")), content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "504", description = "Tiempo de espera agotado", headers = @Header(name = CorrelationIdFilter.HEADER_NAME, schema = @Schema(type = "string")), content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class)))
	})
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<CancellationRequestResponse> initiate(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
					description = "DNI que inicia o recupera la solicitud. Se valida nuevamente en el backend.",
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = StartCancellationRequest.class)))
			@Valid @RequestBody StartCancellationRequest body,
			@Parameter(hidden = true) HttpServletRequest request) {
		String correlationId = String.valueOf(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
		return ResponseEntity.ok(service.initiate(body.dni(), correlationId));
	}
}
