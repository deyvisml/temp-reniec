package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

public record AvailabilityResult(
		AvailabilityOutcome outcome,
		String externalReference,
		String technicalCode) {

	public AvailabilityResult {
		if (outcome == null) throw new IllegalArgumentException("outcome is required");
	}
}
