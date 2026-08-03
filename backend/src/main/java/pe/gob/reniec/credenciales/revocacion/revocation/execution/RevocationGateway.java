package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationResult;

public interface RevocationGateway {
	Result revoke(Command command);

	default boolean isAvailable() {
		return true;
	}

	record Command(String digitalCredentialUuid, int statusListIndex, String dni, String idempotencyKey) { }

	record Result(RevocationResult outcome, String externalReference, String errorCode, Instant respondedAt,
			Integer providerCredentialStatus) {
		public Result(RevocationResult outcome, String externalReference, String errorCode, Instant respondedAt) {
			this(outcome, externalReference, errorCode, respondedAt, null);
		}
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
