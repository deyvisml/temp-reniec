package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

public class IdentityIntegrationException extends RuntimeException {
	private final IdentityFailure failure;

	public IdentityIntegrationException(IdentityFailure failure, String message) {
		super(message);
		this.failure = failure;
	}

	public IdentityIntegrationException(IdentityFailure failure, String message, Throwable cause) {
		super(message, cause);
		this.failure = failure;
	}

	public IdentityFailure failure() { return failure; }
}
