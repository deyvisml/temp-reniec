package pe.gob.reniec.credenciales.revocacion.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.gob.reniec.credenciales.revocacion.shared.error.ApiError;
import pe.gob.reniec.credenciales.revocacion.shared.web.CorrelationIdFilter;

@RestController
@RequestMapping(path = "/api/v1/system", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Estado técnico", description = "Comprobaciones técnicas del backend y sus dependencias.")
public final class SystemStatusController {

	private final SystemStatusService systemStatusService;

	public SystemStatusController(SystemStatusService systemStatusService) {
		this.systemStatusService = systemStatusService;
	}

	@GetMapping("/status")
	@Operation(operationId = "getSystemStatus", summary = "Comprueba la disponibilidad del backend y MySQL",
			description = "Ejecuta una comprobación ligera y actual de MySQL. No devuelve credenciales, coordenadas de conexión ni detalles internos.",
			parameters = @Parameter(name = CorrelationIdFilter.HEADER_NAME, in = ParameterIn.HEADER,
					description = "Identificador opcional de correlación. Debe tener entre 1 y 64 caracteres ASCII válidos.",
					required = false, schema = @Schema(type = "string", maxLength = 64,
							pattern = "[A-Za-z0-9][A-Za-z0-9._-]{0,63}")))
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Backend y base de datos disponibles",
					headers = @Header(name = CorrelationIdFilter.HEADER_NAME,
							description = "Identificador de correlación de la solicitud", schema = @Schema(type = "string")),
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = SystemStatusResponse.class))),
			@ApiResponse(responseCode = "503", description = "MySQL no está disponible",
					headers = @Header(name = CorrelationIdFilter.HEADER_NAME,
							description = "Identificador de correlación de la solicitud", schema = @Schema(type = "string")),
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Error interno controlado",
					headers = @Header(name = CorrelationIdFilter.HEADER_NAME,
							description = "Identificador de correlación de la solicitud", schema = @Schema(type = "string")),
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ApiError.class)))
	})
	public SystemStatusResponse getStatus() {
		return systemStatusService.getStatus();
	}
}
