package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

public enum RevocationFinalOutcome {
	NOT_ELIGIBLE,
	IDENTITY_NOT_VERIFIED,
	REVOCATION_SUCCEEDED,
	REVOCATION_FAILED,
	OUTCOME_UNKNOWN,
	ABANDONED
}
