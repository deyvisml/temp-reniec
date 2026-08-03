package pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials;

public class DigitalCredentialListingException extends RuntimeException {

	private final Reason reason;

	public DigitalCredentialListingException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public Reason reason() { return reason; }

	public enum Reason {
		IDENTITY_REQUIRED,
		NOT_ALLOWED,
		IN_PROGRESS,
		TIMEOUT,
		UNAVAILABLE,
		INVALID_PROVIDER_RESPONSE,
		EMPTY,
		INVALID_SELECTION,
		CONFLICT
	}
}
