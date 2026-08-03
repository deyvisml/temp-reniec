package pe.gob.reniec.credenciales.revocacion.revocation.identity;

public class IdentityIntegrationException extends RuntimeException {
	private final IdentityFailure failure;
	private final String technicalCode;

	public IdentityIntegrationException(IdentityFailure failure, String message) {
		this(failure, failure.name(), message, null);
	}

	public IdentityIntegrationException(IdentityFailure failure, String message, Throwable cause) {
		this(failure, failure.name(), message, cause);
	}

	public IdentityIntegrationException(IdentityFailure failure, String technicalCode,
			String message, Throwable cause) {
		super(message, cause);
		this.failure = failure;
		this.technicalCode = technicalCode;
	}

	public IdentityFailure failure() { return failure; }
	public String technicalCode() { return technicalCode; }
}
