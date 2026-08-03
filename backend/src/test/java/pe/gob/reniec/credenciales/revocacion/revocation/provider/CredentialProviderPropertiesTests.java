package pe.gob.reniec.credenciales.revocacion.revocation.provider;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class CredentialProviderPropertiesTests {

	@Test
	void acceptsHttpOnlyForLoopbackRealDevelopment() {
		CredentialProviderProperties properties = properties("test");
		properties.setBaseUrl(URI.create("http://127.0.0.1:8080"));
		assertThatCode(properties::validate).doesNotThrowAnyException();
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
