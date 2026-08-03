package pe.gob.reniec.credenciales.revocacion.revocation.initiation.antibot;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

class GoogleRecaptchaPropertiesTests {

	@Test
	void acceptsSecureCompleteConfigurationAndNormalizesHostnames() {
		var properties = properties("test-secret", URI.create("https://www.google.com/recaptcha/api/siteverify"),
				Duration.ofSeconds(3), Set.of("LOCALHOST"));

		assertThat(violations(properties)).isZero();
		assertThat(properties.allowedHostnames()).containsExactly("localhost");
	}

	@Test
	void rejectsMissingSecretNonPositiveTimeoutInvalidUriAndEmptyAllowlist() {
		assertThat(violations(properties("", URI.create("https://www.google.com/recaptcha/api/siteverify"),
				Duration.ofSeconds(3), Set.of("localhost")))).isPositive();
		assertThat(violations(properties("test-secret", URI.create("https://www.google.com/recaptcha/api/siteverify"),
				Duration.ZERO, Set.of("localhost")))).isPositive();
		assertThat(violations(properties("test-secret", URI.create("http://localhost/siteverify"),
				Duration.ofSeconds(3), Set.of("localhost")))).isPositive();
		assertThat(violations(properties("test-secret", URI.create("https://www.google.com/recaptcha/api/siteverify"),
				Duration.ofSeconds(3), Set.of()))).isPositive();
	}

	@Test
	void realModeFailsStartupWithoutSecretAndDoesNotEchoASecret() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
				.withUserConfiguration(GoogleRecaptchaConfiguration.class)
				.withPropertyValues(
						"app.recaptcha.mode=google",
						"app.recaptcha.google.verify-uri=https://www.google.com/recaptcha/api/siteverify",
						"app.recaptcha.google.timeout=3s",
						"app.recaptcha.google.allowed-hostnames=localhost")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(String.valueOf(context.getStartupFailure()))
							.contains("app.recaptcha.google")
							.doesNotContain("test-secret");
				});
	}

	private GoogleRecaptchaProperties properties(String secret, URI uri, Duration timeout, Set<String> hostnames) {
		return new GoogleRecaptchaProperties(secret, uri, timeout, hostnames);
	}

	private int violations(GoogleRecaptchaProperties properties) {
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			return factory.getValidator().validate(properties).size();
		}
	}
}
