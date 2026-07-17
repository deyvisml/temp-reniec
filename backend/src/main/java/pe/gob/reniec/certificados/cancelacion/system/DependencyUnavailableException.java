package pe.gob.reniec.certificados.cancelacion.system;

public final class DependencyUnavailableException extends RuntimeException {

	public DependencyUnavailableException(Throwable cause) {
		super("Technical dependency unavailable", cause);
	}
}
