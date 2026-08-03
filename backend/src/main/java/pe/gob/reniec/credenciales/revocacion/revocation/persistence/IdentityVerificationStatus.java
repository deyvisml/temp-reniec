package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

public enum IdentityVerificationStatus {
	STARTED,
	VERIFIED,
	REJECTED,
	CANCELLED,
	EXPIRED,
	IDENTITY_MISMATCH,
	ERROR
}
