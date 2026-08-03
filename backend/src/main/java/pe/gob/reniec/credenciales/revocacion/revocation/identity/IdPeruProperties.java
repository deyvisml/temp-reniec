package pe.gob.reniec.credenciales.revocacion.revocation.identity;

import java.net.URI;
import java.time.Duration;
import java.util.Base64;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.id-peru")
public class IdPeruProperties {

	private static final String AUTH_PATH = "service/auth";
	private static final String TOKEN_PATH = "service/token";
	private static final String USERINFO_PATH = "service/userinfo";
	private static final String JWKS_PATH = "service/jwks";
	private static final URI V1_BASE_URI = URI.create("https://idaas.reniec.gob.pe/");
	private static final URI V2_BASE_URI = URI.create("https://idaas2.reniec.gob.pe/");

	private final Environment environment;
	private final ApplicationUrlProperties applicationUrls;
	private IdPeruMode mode = IdPeruMode.DISABLED;
	private IdPeruVersion version = IdPeruVersion.V1;
	private URI baseUri;
	private String clientId;
	private String clientSecret;
	private String referer;
	private String flowSecret;
	private String mockScenario = "MATCH";
	private String frontendReturnPath = IdPeruFlowSettings.FRONTEND_RETURN_PATH;

	public IdPeruProperties(Environment environment, ApplicationUrlProperties applicationUrls) {
		this.environment = environment;
		this.applicationUrls = applicationUrls;
	}

	@PostConstruct
	void validate() {
		if (environment.acceptsProfiles(Profiles.of("prod")) && mode != IdPeruMode.REAL) {
			throw new IllegalStateException("ID Perú debe ejecutarse en modo real en producción");
		}
		if (mode == IdPeruMode.DISABLED) return;

		requireApplicationUri(applicationUrls.backendBaseUrl(), "app.backend-base-url");
		requireApplicationUri(applicationUrls.frontendBaseUrl(), "app.frontend-base-url");
		requireFlowSecret();
		if (mode == IdPeruMode.REAL) validateReal();
	}

	private void validateReal() {
		requireText(clientId, "client-id");
		if (clientId.length() < 16) throw new IllegalStateException("app.id-peru.client-id no es válido");
		requireText(clientSecret, "client-secret");
		requireText(referer, "referer");
		requireHttpsRoot(effectiveBaseUri(), "base-uri");
		requireApplicationUri(URI.create(referer), "app.id-peru.referer");
		requireApplicationUri(applicationUrls.backendBaseUrl(), "app.backend-base-url");
		requireApplicationUri(applicationUrls.frontendBaseUrl(), "app.frontend-base-url");
		requireRelativePath(frontendReturnPath, "frontend-return-path");
	}

	private void requireFlowSecret() {
		requireText(flowSecret, "flow-secret");
		try {
			if (Base64.getDecoder().decode(flowSecret).length != 32) throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalStateException("app.id-peru.flow-secret debe ser Base64 de 32 bytes");
		}
	}

	private void requireApplicationUri(URI value, String name) {
		if (value == null || !value.isAbsolute() || value.getHost() == null) {
			throw new IllegalStateException(name + " es obligatoria y debe ser absoluta");
		}
		if ("https".equalsIgnoreCase(value.getScheme())) return;
		boolean localProfile = environment.acceptsProfiles(Profiles.of("local", "test"));
		boolean localhost = "localhost".equalsIgnoreCase(value.getHost())
				|| "127.0.0.1".equals(value.getHost()) || "::1".equals(value.getHost());
		if (!localProfile || !"http".equalsIgnoreCase(value.getScheme()) || !localhost) {
			throw new IllegalStateException(name + " debe utilizar HTTPS fuera del entorno local");
		}
	}

	private static void requireHttpsRoot(URI value, String name) {
		requireHttps(value, "app.id-peru." + name);
		if (value.getQuery() != null || value.getFragment() != null
				|| value.getPath() != null && !value.getPath().isBlank() && !"/".equals(value.getPath())) {
			throw new IllegalStateException("app.id-peru." + name + " debe identificar la raíz institucional");
		}
	}

	private static void requireHttps(URI value, String name) {
		if (value == null || !value.isAbsolute() || value.getHost() == null) {
			throw new IllegalStateException(name + " es obligatoria");
		}
		if (!"https".equalsIgnoreCase(value.getScheme())) {
			throw new IllegalStateException(name + " debe utilizar HTTPS");
		}
	}

	private static void requireText(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("app.id-peru." + name + " es obligatorio");
		}
	}

	private static void requireRelativePath(String value, String name) {
		requireText(value, name);
		if (!value.startsWith("/") || value.startsWith("//") || value.contains("?") || value.contains("#")) {
			throw new IllegalStateException("app.id-peru." + name + " debe ser una ruta relativa válida");
		}
	}

	private URI idPeruEndpoint(String path) {
		return normalizedBase(effectiveBaseUri()).resolve(path);
	}

	private URI effectiveBaseUri() {
		if (baseUri != null) return baseUri;
		return version == IdPeruVersion.V2 ? V2_BASE_URI : V1_BASE_URI;
	}

	private static URI appendPath(URI base, String path) {
		return URI.create(stripTrailingSlash(base.toString()) + path);
	}

	private static URI normalizedBase(URI value) {
		return URI.create(stripTrailingSlash(value.toString()) + "/");
	}

	private static String stripTrailingSlash(String value) {
		int end = value.length();
		while (end > 0 && value.charAt(end - 1) == '/') end--;
		return value.substring(0, end);
	}

	byte[] flowSecretBytes() {
		return mode == IdPeruMode.DISABLED ? new byte[32] : Base64.getDecoder().decode(flowSecret);
	}

	public IdPeruMode getMode() { return mode; }
	public void setMode(IdPeruMode mode) { this.mode = mode; }
	public IdPeruVersion getVersion() { return version; }
	public void setVersion(IdPeruVersion version) { this.version = version; }
	public URI getBaseUri() { return effectiveBaseUri(); }
	public void setBaseUri(URI baseUri) { this.baseUri = baseUri; }
	public String getClientId() { return clientId; }
	public void setClientId(String clientId) { this.clientId = clientId; }
	public String getClientSecret() { return clientSecret; }
	public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
	public String getReferer() { return referer; }
	public void setReferer(String referer) { this.referer = referer; }
	public String getFlowSecret() { return flowSecret; }
	public void setFlowSecret(String flowSecret) { this.flowSecret = flowSecret; }
	public String getMockScenario() { return mockScenario; }
	public void setMockScenario(String mockScenario) { this.mockScenario = mockScenario; }
	public String getFrontendReturnPath() { return frontendReturnPath; }
	public void setFrontendReturnPath(String frontendReturnPath) { this.frontendReturnPath = frontendReturnPath; }

	public URI getAuthUri() { return idPeruEndpoint(AUTH_PATH); }
	public URI getTokenUri() { return idPeruEndpoint(TOKEN_PATH); }
	public URI getUserinfoUri() { return idPeruEndpoint(USERINFO_PATH); }
	public URI getJwksUri() { return idPeruEndpoint(JWKS_PATH); }
	public String getIssuer() { return stripTrailingSlash(effectiveBaseUri().toString()); }
	public String getScope() { return version == IdPeruVersion.V1 ? "openid profile" : "openid"; }
	public boolean usesPkce() { return version == IdPeruVersion.V2; }
	public boolean requiresSessionState() { return version == IdPeruVersion.V2; }
	public URI getRedirectUri() { return appendPath(applicationUrls.backendBaseUrl(), IdPeruFlowSettings.CALLBACK_PATH); }
	public URI getFrontendReturnUri() { return appendPath(applicationUrls.frontendBaseUrl(), frontendReturnPath); }
	public String getAcrValues() { return IdPeruFlowSettings.ACR_VALUES; }
	public Duration getConnectTimeout() { return IdPeruFlowSettings.CONNECT_TIMEOUT; }
	public Duration getReadTimeout() { return IdPeruFlowSettings.READ_TIMEOUT; }
	public Duration getStateTtl() { return IdPeruFlowSettings.STATE_TTL; }
	public Duration getJwksTtl() { return IdPeruFlowSettings.JWKS_TTL; }
	public boolean isCookieSecure() { return !environment.acceptsProfiles(Profiles.of("local", "test")); }
}
