package pe.gob.reniec.certificados.cancelacion.cancellation.confirmation;

public final class CancellationConfirmationException extends RuntimeException {

	private final Reason reason;

	public CancellationConfirmationException(Reason reason, String message) {
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
		CONFLICT
	}
}
