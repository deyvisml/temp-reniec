package pe.gob.reniec.credenciales.revocacion.revocation.provider;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class CredentialProviderPropertiesTests {

	@Test
	void localAndProductionDisableRecaptchaWhilePortsRemainSeparated() throws IOException {
		String local = Files.readString(Path.of("src/main/resources/application-local.yml"));
		String common = Files.readString(Path.of("src/main/resources/application.yml"));
		String production = Files.readString(Path.of("src/main/resources/application-prod.yml"));

		assertThat(local)
				.contains("recaptcha:", "mode: disabled")
				.contains("mode: ${CREDENTIAL_PROVIDER_MODE:real}")
				.contains("base-url: ${CREDENTIAL_PROVIDER_BASE_URL:http://localhost:8081}")
				.contains("backend-base-url: ${APP_BACKEND_BASE_URL:http://localhost:${SERVER_PORT:8080}}");
		assertThat(production).contains("recaptcha:", "mode: disabled");
		assertThat(common).contains("port: ${SERVER_PORT:8080}");
	}

	@Test
	void acceptsHttpOnlyForLoopbackRealDevelopment() {
		CredentialProviderProperties properties = properties("test");
		properties.setBaseUrl(URI.create("http://127.0.0.1:8080"));
		assertThatCode(properties::validate).doesNotThrowAnyException();
	}

	@Test
	void acceptsDockerHostGatewayOnlyForLocalProfile() {
		CredentialProviderProperties local = properties("local");
		local.setBaseUrl(URI.create("http://host.docker.internal:8081"));
		assertThatCode(local::validate).doesNotThrowAnyException();

		CredentialProviderProperties test = properties("test");
		test.setBaseUrl(URI.create("http://host.docker.internal:8081"));
		assertThatThrownBy(test::validate).isInstanceOf(IllegalStateException.class);

		CredentialProviderProperties arbitraryLocalHost = properties("local");
		arbitraryLocalHost.setBaseUrl(URI.create("http://provider.internal:8081"));
		assertThatThrownBy(arbitraryLocalHost::validate).isInstanceOf(IllegalStateException.class);

		CredentialProviderProperties production = properties("prod");
		production.setBaseUrl(URI.create("http://host.docker.internal:8081"));
		assertThatThrownBy(production::validate).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void productionRequiresRealHttpsAndCredentials() {
		CredentialProviderProperties disabled = properties("prod");
		disabled.setMode(CredentialProviderProperties.Mode.DISABLED);
		assertThatThrownBy(disabled::validate).isInstanceOf(IllegalStateException.class);

		CredentialProviderProperties http = properties("prod");
		http.setBaseUrl(URI.create("http://provider.example"));
		assertThatThrownBy(http::validate).isInstanceOf(IllegalStateException.class);

		CredentialProviderProperties https = properties("prod");
		https.setBaseUrl(URI.create("https://provider.example"));
		assertThatCode(https::validate).doesNotThrowAnyException();
	}

	@Test
	void rejectsMissingSecretsInvalidUrlsAndTimeouts() {
		CredentialProviderProperties missingKey = properties("test");
		missingKey.setApiKey(" ");
		assertThatThrownBy(missingKey::validate).isInstanceOf(IllegalStateException.class);

		CredentialProviderProperties relative = properties("test");
		relative.setBaseUrl(URI.create("/provider"));
		assertThatThrownBy(relative::validate).isInstanceOf(IllegalStateException.class);

		CredentialProviderProperties timeout = properties("test");
		timeout.setConnectTimeout(Duration.ZERO);
		assertThatThrownBy(timeout::validate).isInstanceOf(IllegalStateException.class);
	}

	private static CredentialProviderProperties properties(String profile) {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles(profile);
		CredentialProviderProperties properties = new CredentialProviderProperties(environment);
		properties.setMode(CredentialProviderProperties.Mode.REAL);
		properties.setBaseUrl(URI.create("https://provider.example"));
		properties.setApiKey("test-key");
		return properties;
	}
}
