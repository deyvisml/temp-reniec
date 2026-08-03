package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

public enum FlowSessionStatus {
	PENDING_IDENTITY,
	IDENTITY_VERIFIED,
	INVALIDATED,
	EXPIRED
}
