package pe.gob.reniec.certificados.cancelacion.cancellation.reason;

public final class CancellationReasonException extends RuntimeException {
	private final Reason reason;

	public CancellationReasonException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public Reason reason() { return reason; }

	public enum Reason {
		IDENTITY_REQUIRED,
		NOT_ALLOWED,
		INVALID_SELECTION,
		INVALID_REASON,
		CONFLICT
	}
}
