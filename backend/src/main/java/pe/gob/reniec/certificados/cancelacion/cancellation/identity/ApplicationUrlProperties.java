package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class ApplicationUrlProperties {

	private URI frontendBaseUrl;
	private URI backendBaseUrl;

	public URI frontendBaseUrl() {
		return frontendBaseUrl;
	}

	public void setFrontendBaseUrl(URI frontendBaseUrl) {
		this.frontendBaseUrl = frontendBaseUrl;
	}

	public URI backendBaseUrl() {
		return backendBaseUrl;
	}

	public void setBackendBaseUrl(URI backendBaseUrl) {
		this.backendBaseUrl = backendBaseUrl;
	}
}
