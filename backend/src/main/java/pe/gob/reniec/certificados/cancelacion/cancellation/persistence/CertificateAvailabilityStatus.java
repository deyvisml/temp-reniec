package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

public enum CertificateAvailabilityStatus {
	AVAILABLE,
	NO_LONGER_AVAILABLE,
	REVOCATION_PENDING,
	REVOKED,
	REVOCATION_FAILED,
	OUTCOME_UNKNOWN
}
