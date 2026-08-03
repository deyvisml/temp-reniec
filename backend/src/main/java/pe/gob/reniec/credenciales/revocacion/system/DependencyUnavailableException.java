package pe.gob.reniec.credenciales.revocacion.system;

public final class DependencyUnavailableException extends RuntimeException {

	public DependencyUnavailableException(Throwable cause) {
		super("Technical dependency unavailable", cause);
	}
}
