package pe.gob.reniec.certificados.cancelacion.cancellation.initiation.antibot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.recaptcha.mode", havingValue = "disabled", matchIfMissing = true)
final class UnavailableAntiBotVerificationAdapter implements AntiBotVerificationPort {

	@Override
	public void verify(String token) {
		throw new RecaptchaVerificationException(RecaptchaFailure.UNAVAILABLE);
	}
}
