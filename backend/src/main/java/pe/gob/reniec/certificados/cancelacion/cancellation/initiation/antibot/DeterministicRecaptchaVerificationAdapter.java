package pe.gob.reniec.certificados.cancelacion.cancellation.initiation.antibot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
@ConditionalOnProperty(name = "app.recaptcha.mode", havingValue = "mock")
final class DeterministicRecaptchaVerificationAdapter implements AntiBotVerificationPort {

	static final String VALID_TOKEN = "test-recaptcha-valid";
	static final String LOCAL_BYPASS_TOKEN = "local-development-bypass";

	@Override
	public void verify(String token) {
		if (token == null || token.isBlank()) throw new RecaptchaVerificationException(RecaptchaFailure.REQUIRED);
		switch (token) {
			case VALID_TOKEN, LOCAL_BYPASS_TOKEN -> { }
			case "test-recaptcha-expired" -> throw new RecaptchaVerificationException(RecaptchaFailure.EXPIRED_OR_DUPLICATE);
			case "test-recaptcha-timeout" -> throw new RecaptchaVerificationException(RecaptchaFailure.TIMEOUT);
			case "test-recaptcha-unavailable" -> throw new RecaptchaVerificationException(RecaptchaFailure.UNAVAILABLE);
			case "test-recaptcha-invalid-response" -> throw new RecaptchaVerificationException(RecaptchaFailure.INVALID_RESPONSE);
			default -> throw new RecaptchaVerificationException(RecaptchaFailure.REJECTED);
		}
	}
}
