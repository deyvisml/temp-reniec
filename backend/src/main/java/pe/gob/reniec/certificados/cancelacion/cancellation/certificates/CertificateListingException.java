package pe.gob.reniec.certificados.cancelacion.cancellation.certificates;

public class CertificateListingException extends RuntimeException {

	private final Reason reason;

	public CertificateListingException(Reason reason, String message) {
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
