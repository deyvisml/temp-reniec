package pe.gob.reniec.credenciales.revocacion.revocation.initiation.antibot;

public enum RecaptchaFailure {
	REQUIRED,
	REJECTED,
	EXPIRED_OR_DUPLICATE,
	UNAVAILABLE,
	TIMEOUT,
	INVALID_RESPONSE
}
