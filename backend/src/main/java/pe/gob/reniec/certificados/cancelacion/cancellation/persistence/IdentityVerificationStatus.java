package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

public enum IdentityVerificationStatus {
	STARTED,
	VERIFIED,
	REJECTED,
	CANCELLED,
	EXPIRED,
	IDENTITY_MISMATCH,
	ERROR
}
