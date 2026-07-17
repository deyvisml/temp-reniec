package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

public record EligibilityGatewayResult(
		EligibilityOutcome outcome,
		String externalReference,
		String technicalCode) {

	public EligibilityGatewayResult {
		if (outcome == null) throw new IllegalArgumentException("outcome is required");
	}
}
