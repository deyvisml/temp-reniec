package pe.gob.reniec.credenciales.revocacion.revocation.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

class IdPeruConfigurationTests {

	private static final String FLOW_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
	private static final List<String> RETIRED_VARIABLES = List.of(
			"ID_PERU_BASE_URI", "ID_PERU_AUTH_URI", "ID_PERU_TOKEN_URI", "ID_PERU_USERINFO_URI",
			"ID_PERU_LOGOUT_URI", "ID_PERU_JWKS_URI", "ID_PERU_ISSUER", "ID_PERU_REDIRECT_URI",
			"ID_PERU_FRONTEND_RETURN_URI", "ID_PERU_ACR_VALUES", "ID_PERU_MAX_AGE",
			"ID_PERU_CONNECT_TIMEOUT", "ID_PERU_READ_TIMEOUT", "ID_PERU_STATE_TTL",
			"ID_PERU_IDENTITY_INIT_TTL", "ID_PERU_FLOW_AUTHORIZATION_TTL", "ID_PERU_JWKS_TTL",
			"ID_PERU_PKCE_ENCRYPTION_KEY", "ID_PERU_FLOW_SIGNING_KEY", "ID_PERU_COOKIE_SECURE",
			"ID_PERU_COOKIE_NAME");

	@Test
	void derivesV1ProviderUrisForTheLocallyAuthorizedCredentials() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("local");
		ApplicationUrlProperties urls = new ApplicationUrlProperties();
		urls.setFrontendBaseUrl(URI.create("http://localhost:3000"));
		urls.setBackendBaseUrl(URI.create("http://localhost:8080"));
		IdPeruProperties properties = new IdPeruProperties(environment, urls);
		properties.setMode(IdPeruMode.REAL);
		properties.setVersion(IdPeruVersion.V1);
		properties.setClientId("1234567890abcdef-client-id");
		properties.setClientSecret("secret-value");
		properties.setReferer("http://localhost:3000/autorizacion");
		properties.setFlowSecret(FLOW_SECRET);

		properties.validate();

		assertThat(properties.getAuthUri()).isEqualTo(URI.create("https://idaas.reniec.gob.pe/service/auth"));
		assertThat(properties.getTokenUri()).isEqualTo(URI.create("https://idaas.reniec.gob.pe/service/token"));
		assertThat(properties.getUserinfoUri()).isEqualTo(URI.create("https://idaas.reniec.gob.pe/service/userinfo"));
		assertThat(properties.getScope()).isEqualTo("openid profile");
		assertThat(properties.usesPkce()).isFalse();
	}

	@Test
	void derivesProviderAndApplicationUrisFromThreeBaseValues() {
		IdPeruProperties properties = realProperties();
		properties.validate();

		assertThat(properties.getAuthUri()).isEqualTo(URI.create("https://idaas2.reniec.gob.pe/service/auth"));
		assertThat(properties.getTokenUri()).isEqualTo(URI.create("https://idaas2.reniec.gob.pe/service/token"));
		assertThat(properties.getUserinfoUri()).isEqualTo(URI.create("https://idaas2.reniec.gob.pe/service/userinfo"));
		assertThat(properties.getJwksUri()).isEqualTo(URI.create("https://idaas2.reniec.gob.pe/service/jwks"));
		assertThat(properties.getIssuer()).isEqualTo("https://idaas2.reniec.gob.pe");
		assertThat(properties.getRedirectUri()).isEqualTo(
				URI.create("https://api.example.test/api/v1/idperu/callback"));
		assertThat(properties.getFrontendReturnUri()).isEqualTo(URI.create("https://app.example.test/revocacion"));
	}

	@Test
	void rejectsMissingInvalidOrInsecureRealConfigurationWithoutEchoingSecrets() {
		IdPeruProperties missingSecret = realProperties();
		missingSecret.setFlowSecret(null);
		assertThatThrownBy(missingSecret::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("flow-secret")
				.hasMessageNotContaining(FLOW_SECRET);

		IdPeruProperties invalidSecret = realProperties();
		invalidSecret.setFlowSecret("not-base64");
		assertThatThrownBy(invalidSecret::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("app.id-peru.flow-secret debe ser Base64 de 32 bytes")
				.hasMessageNotContaining("not-base64");

		IdPeruProperties insecure = realProperties();
		insecure.setBaseUri(URI.create("http://idaas2.reniec.gob.pe/"));
		assertThatThrownBy(insecure::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("HTTPS");
	}

	@Test
	void localMockNeedsNoInstitutionalConfigurationAndUsesInternalSettings() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("local");
		ApplicationUrlProperties urls = new ApplicationUrlProperties();
		urls.setFrontendBaseUrl(URI.create("http://localhost:3000"));
		urls.setBackendBaseUrl(URI.create("http://localhost:8080"));
		IdPeruProperties properties = new IdPeruProperties(environment, urls);
		properties.setMode(IdPeruMode.MOCK);
		properties.setFlowSecret(FLOW_SECRET);

		properties.validate();

		assertThat(properties.isCookieSecure()).isFalse();
		assertThat(properties.getAcrValues()).isEqualTo("face_mobile");
		assertThat(properties.getConnectTimeout()).isEqualTo(java.time.Duration.ofSeconds(3));
		assertThat(properties.getReadTimeout()).isEqualTo(java.time.Duration.ofSeconds(5));
		assertThat(properties.getStateTtl()).isEqualTo(java.time.Duration.ofMinutes(5));
		assertThat(properties.getJwksTtl()).isEqualTo(java.time.Duration.ofMinutes(15));
	}

	@Test
	void localRealModeAcceptsLocalApplicationUrlsAndKeepsProviderTransportSecure() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("local");
		ApplicationUrlProperties urls = new ApplicationUrlProperties();
		urls.setFrontendBaseUrl(URI.create("http://localhost:3000"));
		urls.setBackendBaseUrl(URI.create("http://localhost:8080"));
		IdPeruProperties properties = new IdPeruProperties(environment, urls);
		properties.setMode(IdPeruMode.REAL);
		properties.setVersion(IdPeruVersion.V2);
		properties.setBaseUri(URI.create("https://idaas2.reniec.gob.pe/"));
		properties.setClientId("1234567890abcdef-client-id");
		properties.setClientSecret("secret-value");
		properties.setReferer("http://localhost:3000/autorizacion");
		properties.setFlowSecret(FLOW_SECRET);
		properties.setFrontendReturnPath("/autorizacion");

		properties.validate();

		assertThat(properties.getRedirectUri()).isEqualTo(
				URI.create("http://localhost:8080/api/v1/idperu/callback"));
		assertThat(properties.getFrontendReturnUri()).isEqualTo(
				URI.create("http://localhost:3000/autorizacion"));
		assertThat(properties.isCookieSecure()).isFalse();
	}

	@Test
	void callbackControllerUsesTheEnvironmentSpecificRegisteredPath() throws Exception {
		GetMapping getMapping = IdentityVerificationController.class
				.getMethod("callbackGet", String.class, String.class, String.class, String.class)
				.getAnnotation(GetMapping.class);
		PostMapping postMapping = IdentityVerificationController.class
				.getMethod("callbackPost", String.class, String.class, String.class, String.class)
				.getAnnotation(PostMapping.class);

		assertThat(getMapping.path()).containsExactly("/api/v1/idperu/callback");
		assertThat(postMapping.path()).containsExactly("/api/v1/idperu/callback");
	}

	@Test
	void productionKeepsCanonicalRoutesAndRejectsAnHttpReferer() {
		IdPeruProperties properties = realProperties();
		properties.validate();

		assertThat(properties.getRedirectUri()).isEqualTo(
				URI.create("https://api.example.test/api/v1/idperu/callback"));
		assertThat(properties.getFrontendReturnUri()).isEqualTo(URI.create("https://app.example.test/revocacion"));

		properties.setReferer("http://localhost:3000/autorizacion");
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("HTTPS");
	}

	@Test
	void productionRejectsAnyModeOtherThanReal() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod");
		ApplicationUrlProperties urls = new ApplicationUrlProperties();
		IdPeruProperties properties = new IdPeruProperties(environment, urls);
		properties.setMode(IdPeruMode.MOCK);

		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("modo real");
	}

	@Test
	void providerAdaptersAreMutuallyExclusiveByMode() {
		assertConditionalMode(RealIdPeruAdapter.class, "real");
		assertConditionalMode(MockCitizenIdentityProviderAdapter.class, "mock");
		assertConditionalMode(DisabledCitizenIdentityProviderAdapter.class, "disabled");
	}

	@Test
	void localYamlDefaultsToMockAndAllowsEnvironmentOverrideToReal() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		new YamlPropertySourceLoader().load("local",
				new FileSystemResource("src/main/resources/application-local.yml"))
				.forEach(source -> environment.getPropertySources().addLast(source));

		assertThat(environment.getProperty("app.id-peru.mode")).isEqualTo("mock");

		environment.getPropertySources().addFirst(
				new MapPropertySource("id-peru-real-local", Map.of("ID_PERU_MODE", "real")));

		assertThat(environment.getProperty("app.id-peru.mode")).isEqualTo("real");
	}

	@Test
	void configurationFilesExposeOnlyTheApprovedIdPeruEnvironmentVariables() throws Exception {
		String localConfiguration = Files.readString(Path.of("src/main/resources/application-local.yml"));
		String configuration = Files.readString(Path.of("src/main/resources/application.yml"))
				+ localConfiguration
				+ Files.readString(Path.of("src/main/resources/application-test.yml"))
				+ Files.readString(Path.of("src/main/resources/application-prod.yml"))
				+ Files.readString(Path.of(".env.example"));

		assertThat(configuration).contains("APP_FRONTEND_BASE_URL", "APP_BACKEND_BASE_URL", "ID_PERU_MODE",
				"ID_PERU_CLIENT_ID", "ID_PERU_CLIENT_SECRET", "ID_PERU_REFERER", "ID_PERU_FLOW_SECRET");
		assertThat(localConfiguration)
				.contains("mode: ${ID_PERU_MODE:mock}")
				.contains("frontend-return-path: /autorizacion")
				.contains("referer: ${ID_PERU_REFERER:http://localhost:3000/autorizacion}")
				.contains("frontend-base-url: ${APP_FRONTEND_BASE_URL:http://localhost:3000}")
				.contains("backend-base-url: ${APP_BACKEND_BASE_URL:http://localhost:${SERVER_PORT:8080}}");
		assertThat(localConfiguration).contains("version: v1");
		assertThat(Files.readString(Path.of("src/main/resources/application-prod.yml")))
				.contains("version: v2");
		for (String retired : RETIRED_VARIABLES) assertThat(configuration).doesNotContain(retired);
	}

	private static IdPeruProperties realProperties() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod");
		ApplicationUrlProperties urls = new ApplicationUrlProperties();
		urls.setFrontendBaseUrl(URI.create("https://app.example.test/"));
		urls.setBackendBaseUrl(URI.create("https://api.example.test/"));
		IdPeruProperties properties = new IdPeruProperties(environment, urls);
		properties.setMode(IdPeruMode.REAL);
		properties.setVersion(IdPeruVersion.V2);
		properties.setBaseUri(URI.create("https://idaas2.reniec.gob.pe/"));
		properties.setClientId("1234567890abcdef-client-id");
		properties.setClientSecret("secret-value");
		properties.setReferer("https://app.example.test");
		properties.setFlowSecret(FLOW_SECRET);
		return properties;
	}

	private static void assertConditionalMode(Class<?> adapterType, String expectedMode) {
		ConditionalOnProperty condition = adapterType.getAnnotation(ConditionalOnProperty.class);
		assertThat(condition).isNotNull();
		assertThat(condition.prefix()).isEqualTo("app.id-peru");
		assertThat(condition.name()).containsExactly("mode");
		assertThat(condition.havingValue()).isEqualTo(expectedMode);
	}
}
