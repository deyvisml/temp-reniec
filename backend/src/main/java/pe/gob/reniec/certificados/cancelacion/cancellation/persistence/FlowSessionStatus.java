package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

public enum FlowSessionStatus {
	PENDING_IDENTITY,
	IDENTITY_VERIFIED,
	INVALIDATED,
	EXPIRED
}
