package pe.gob.reniec.certificados.cancelacion.system;

import io.swagger.v3.oas.annotations.Operation;
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

import pe.gob.reniec.certificados.cancelacion.shared.error.ApiError;
import pe.gob.reniec.certificados.cancelacion.shared.web.CorrelationIdFilter;

@RestController
@RequestMapping(path = "/api/v1/system", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Estado técnico")
public final class SystemStatusController {

	private final SystemStatusService systemStatusService;

	public SystemStatusController(SystemStatusService systemStatusService) {
		this.systemStatusService = systemStatusService;
	}

	@GetMapping("/status")
	@Operation(summary = "Comprueba la disponibilidad del backend y MySQL")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Backend y base de datos disponibles",
					headers = @Header(name = CorrelationIdFilter.HEADER_NAME,
							description = "Identificador de correlación de la solicitud"),
					content = @Content(schema = @Schema(implementation = SystemStatusResponse.class))),
			@ApiResponse(responseCode = "503", description = "Dependencia técnica no disponible",
					headers = @Header(name = CorrelationIdFilter.HEADER_NAME,
							description = "Identificador de correlación de la solicitud"),
					content = @Content(schema = @Schema(implementation = ApiError.class)))
	})
	public SystemStatusResponse getStatus() {
		return systemStatusService.getStatus();
	}
}
