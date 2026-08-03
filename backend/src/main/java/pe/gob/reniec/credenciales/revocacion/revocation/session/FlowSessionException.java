package pe.gob.reniec.credenciales.revocacion.revocation.session;

public class FlowSessionException extends RuntimeException {
	private final Reason reason;
	public FlowSessionException(Reason reason) { super(reason.name()); this.reason = reason; }
	public Reason reason() { return reason; }
	public enum Reason { REQUIRED, EXPIRED, INVALID, REFRESH_CONFLICT, REPLAYED, FORBIDDEN, ALREADY_ACTIVE }
}
