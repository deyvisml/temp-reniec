package pe.gob.reniec.certificados.cancelacion.cancellation.initiation;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public record AvailabilityResult(
		AvailabilityOutcome outcome,
		String externalReference,
		String technicalCode) {

	public AvailabilityResult {
		outcome = Objects.requireNonNull(outcome, "outcome");
		externalReference = optionalAsciiText(externalReference, "externalReference", 128);
		technicalCode = optionalAsciiText(technicalCode, "technicalCode", 64);
		if ((outcome == AvailabilityOutcome.UNAVAILABLE || outcome == AvailabilityOutcome.ERROR)
				&& technicalCode == null) {
			throw new IllegalArgumentException("technicalCode is required for a failed availability check");
		}
	}

	private static String optionalAsciiText(String value, String name, int maxLength) {
		if (value == null) return null;
		String text = value.trim();
		if (text.isEmpty()) return null;
		if (text.length() > maxLength) throw new IllegalArgumentException(name + " is too long");
		if (!StandardCharsets.US_ASCII.newEncoder().canEncode(text)) {
			throw new IllegalArgumentException(name + " must contain ASCII characters");
		}
		return text;
	}
}
