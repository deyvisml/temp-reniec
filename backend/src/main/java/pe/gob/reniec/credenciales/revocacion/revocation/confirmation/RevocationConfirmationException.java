package pe.gob.reniec.credenciales.revocacion.revocation.confirmation;

public final class RevocationConfirmationException extends RuntimeException {

	private final Reason reason;

	public RevocationConfirmationException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public Reason reason() { return reason; }

	public enum Reason {
		IDENTITY_REQUIRED,
		NOT_ALLOWED,
		INVALID_REASON,
		INVALID_SELECTION,
		CONSENT_REQUIRED,
		CONSENT_CHANGED,
		CONFLICT,
		DEPENDENCY_UNAVAILABLE
	}
}
