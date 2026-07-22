package pe.gob.reniec.certificados.cancelacion.cancellation.initiation.antibot;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.recaptcha.google")
public record GoogleRecaptchaProperties(
		@NotBlank @Size(max = 512) String secretKey,
		@NotNull URI verifyUri,
		@NotNull Duration timeout,
		@NotEmpty Set<@NotBlank @Size(max = 253) @Pattern(regexp = "[A-Za-z0-9.-]+") String> allowedHostnames) {

	public GoogleRecaptchaProperties {
		if (allowedHostnames != null) {
			allowedHostnames = allowedHostnames.stream()
					.map(hostname -> hostname.trim().toLowerCase(Locale.ROOT))
					.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
		}
	}

	@AssertTrue(message = "must use an HTTPS URI")
	public boolean isVerifyUriSecure() {
		return verifyUri != null && "https".equalsIgnoreCase(verifyUri.getScheme()) && verifyUri.getHost() != null;
	}

	@AssertTrue(message = "must be greater than zero")
	public boolean isTimeoutPositive() {
		return timeout != null && !timeout.isZero() && !timeout.isNegative();
	}
}
