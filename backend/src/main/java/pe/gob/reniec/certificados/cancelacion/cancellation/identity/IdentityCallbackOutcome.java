package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import java.util.Locale;
import java.util.Optional;

public enum IdentityCallbackOutcome {
	CANCELLED,
	REJECTED,
	IDENTITY_MISMATCH,
	EXPIRED,
	TIMEOUT,
	UNAVAILABLE,
	ERROR;

	static IdentityCallbackOutcome fromStatus(String status) {
		if (status == null || status.isBlank() || "VERIFIED".equals(status)) return null;
		return parse(status).orElse(ERROR);
	}

	static IdentityCallbackOutcome fromFailure(IdentityFailure failure) {
		return switch (failure) {
			case CANCELLED -> CANCELLED;
			case REJECTED, TOKEN_REJECTED -> REJECTED;
			case IDENTITY_MISMATCH -> IDENTITY_MISMATCH;
			case STATE_EXPIRED -> EXPIRED;
			case TIMEOUT -> TIMEOUT;
			case UNAVAILABLE -> UNAVAILABLE;
			default -> ERROR;
		};
	}

	static Optional<IdentityCallbackOutcome> parse(String value) {
		if (value == null || value.isBlank()) return Optional.empty();
		try {
			return Optional.of(valueOf(value.toUpperCase(Locale.ROOT)));
		}
		catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}
}
