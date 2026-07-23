package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RestController
@RequestMapping("/api/v1/identity-verifications/mock")
@ConditionalOnProperty(prefix = "app.id-peru", name = "mode", havingValue = "mock")
@Tag(name = "Simulador ID Perú", description = "Proveedor determinista disponible solo en desarrollo y pruebas.")
public class MockIdentityProviderController {
	private final IdPeruProperties properties;
	public MockIdentityProviderController(IdPeruProperties properties) { this.properties = properties; }

	@GetMapping(path = "/authorize", produces = MediaType.TEXT_HTML_VALUE)
	@io.swagger.v3.oas.annotations.Operation(operationId = "simulateIdPeruAuthorization",
			summary = "Simula el proveedor ID Perú",
			description = "Ruta disponible únicamente en modo mock local o de pruebas.")
	public String authorize(@RequestParam String state) {
		String scenario = properties.getMockScenario().toUpperCase();
		String outcome = switch (scenario) {
			case "CANCELLED" -> "<input type='hidden' name='error' value='access_denied'>";
			case "REJECTED" -> "<input type='hidden' name='error' value='login_required'>";
			default -> "<input type='hidden' name='code' value='mock-code'>";
		};
		return "<!doctype html><html lang='es'><body><p>Procesando autenticación simulada…</p>"
				+ "<form id='callback' method='post' action='" + escape(properties.getRedirectUri().toString()) + "'>"
				+ "<input type='hidden' name='state' value='" + escape(state) + "'>" + outcome
				+ "<input type='hidden' name='session_state' value='mock-session'></form>"
				+ "<script>document.getElementById('callback').submit()</script></body></html>";
	}

	private static String escape(String value) {
		return value.replace("&", "&amp;").replace("'", "&#39;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
