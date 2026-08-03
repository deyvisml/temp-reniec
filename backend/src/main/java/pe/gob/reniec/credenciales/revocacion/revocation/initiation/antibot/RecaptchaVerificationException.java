package pe.gob.reniec.credenciales.revocacion.revocation.initiation.antibot;

public final class RecaptchaVerificationException extends RuntimeException {

	private final RecaptchaFailure failure;

	public RecaptchaVerificationException(RecaptchaFailure failure) {
		super("reCAPTCHA verification failed: " + failure.name());
		this.failure = failure;
	}

	public RecaptchaVerificationException(RecaptchaFailure failure, Throwable cause) {
		super("reCAPTCHA verification failed: " + failure.name(), cause);
		this.failure = failure;
	}

	public RecaptchaFailure failure() {
		return failure;
	}
}
