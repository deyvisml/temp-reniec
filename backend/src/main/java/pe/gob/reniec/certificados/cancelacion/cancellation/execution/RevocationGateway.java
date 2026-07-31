package pe.gob.reniec.certificados.cancelacion.cancellation.execution;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationResult;

public interface RevocationGateway {
	Result revoke(String certificateUuid, String idempotencyKey);

	default boolean isAvailable() {
		return true;
	}

	record Result(RevocationResult outcome, String externalReference, String errorCode, Instant respondedAt) {
		public Result {
			outcome = Objects.requireNonNull(outcome, "outcome");
			externalReference = optionalAscii(externalReference, "externalReference", 128);
			errorCode = optionalAscii(errorCode, "errorCode", 64);
			respondedAt = Objects.requireNonNull(respondedAt, "respondedAt");
		}

		private static String optionalAscii(String value, String name, int maxLength) {
			if (value == null) return null;
			String normalized = value.trim();
			if (normalized.isEmpty()) return null;
			if (normalized.length() > maxLength
					|| !StandardCharsets.US_ASCII.newEncoder().canEncode(normalized)) {
				throw new IllegalArgumentException(name + " is invalid");
			}
			return normalized;
		}
	}
}
