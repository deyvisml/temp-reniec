package pe.gob.reniec.credenciales.revocacion.revocation.provider;

import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("app.credential-provider")
public class CredentialProviderProperties {

	private final Environment environment;
	private Mode mode = Mode.DISABLED;
	private URI baseUrl;
	private String apiKey;
	private Duration connectTimeout = Duration.ofSeconds(3);
	private Duration readTimeout = Duration.ofSeconds(5);

	public CredentialProviderProperties(Environment environment) {
		this.environment = environment;
	}

	@PostConstruct
	void validate() {
		requirePositive(connectTimeout, "app.credential-provider.connect-timeout");
		requirePositive(readTimeout, "app.credential-provider.read-timeout");
		if (environment.acceptsProfiles(Profiles.of("prod")) && mode != Mode.REAL) {
			throw new IllegalStateException("app.credential-provider.mode must be real in production");
		}
		if (mode != Mode.REAL) return;
		if (baseUrl == null || !baseUrl.isAbsolute() || baseUrl.getHost() == null
				|| baseUrl.getQuery() != null || baseUrl.getFragment() != null) {
			throw new IllegalStateException("app.credential-provider.base-url must be an absolute service URL");
		}
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("app.credential-provider.api-key is required in real mode");
		}
		String scheme = baseUrl.getScheme();
		if ("https".equalsIgnoreCase(scheme)) return;
		if (!"http".equalsIgnoreCase(scheme) || !isPermittedLocalHttpHost(baseUrl.getHost())) {
			throw new IllegalStateException("app.credential-provider.base-url must use HTTPS outside local development");
		}
	}

	private boolean isPermittedLocalHttpHost(String host) {
		boolean loopbackDevelopment = environment.acceptsProfiles(Profiles.of("local", "test")) && isLoopback(host);
		boolean localDockerGateway = environment.acceptsProfiles(Profiles.of("local"))
				&& "host.docker.internal".equalsIgnoreCase(host);
		return loopbackDevelopment || localDockerGateway;
	}

	private static boolean isLoopback(String host) {
		try {
			return "localhost".equalsIgnoreCase(host) || InetAddress.getByName(host).isLoopbackAddress();
		}
		catch (Exception exception) {
			return false;
		}
	}

	private static void requirePositive(Duration value, String property) {
		if (value == null || value.isZero() || value.isNegative()) {
			throw new IllegalStateException(property + " must be positive");
		}
	}

	public Mode getMode() { return mode; }
	public void setMode(Mode mode) { this.mode = mode; }
	public URI getBaseUrl() { return baseUrl; }
	public void setBaseUrl(URI baseUrl) { this.baseUrl = baseUrl; }
	public String getApiKey() { return apiKey; }
	public void setApiKey(String apiKey) { this.apiKey = apiKey; }
	public Duration getConnectTimeout() { return connectTimeout; }
	public void setConnectTimeout(Duration value) { connectTimeout = value; }
	public Duration getReadTimeout() { return readTimeout; }
	public void setReadTimeout(Duration value) { readTimeout = value; }

	public enum Mode { MOCK, REAL, DISABLED }
}
