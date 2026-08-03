package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

public enum DigitalCredentialAvailabilityStatus {
	AVAILABLE,
	NO_LONGER_AVAILABLE,
	REVOCATION_PENDING,
	REVOKED,
	REVOCATION_FAILED,
	OUTCOME_UNKNOWN
}
