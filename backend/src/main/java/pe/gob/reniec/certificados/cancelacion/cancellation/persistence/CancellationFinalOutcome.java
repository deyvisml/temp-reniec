package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

public enum CancellationFinalOutcome {
	NOT_ELIGIBLE,
	IDENTITY_NOT_VERIFIED,
	REVOCATION_SUCCEEDED,
	REVOCATION_FAILED,
	OUTCOME_UNKNOWN,
	ABANDONED
}
