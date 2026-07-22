package pe.gob.reniec.certificados.cancelacion.cancellation.initiation.antibot;

public enum RecaptchaFailure {
	REQUIRED,
	REJECTED,
	EXPIRED_OR_DUPLICATE,
	UNAVAILABLE,
	TIMEOUT,
	INVALID_RESPONSE
}
