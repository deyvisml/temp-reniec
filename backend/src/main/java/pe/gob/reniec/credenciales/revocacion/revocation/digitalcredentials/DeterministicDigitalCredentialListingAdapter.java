package pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials;

import java.time.Instant;
import java.util.List;

import pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialListingResult.ListedDigitalCredential;
import pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialListingResult.Outcome;

final class DeterministicDigitalCredentialListingAdapter implements DigitalCredentialListingPort {

	private static final Instant FIRST_DATE = Instant.parse("2024-07-15T15:24:00Z");
	private static final Instant REVOKED_DATE = Instant.parse("2025-11-18T16:30:00Z");

	@Override
	public DigitalCredentialListingResult listDigitalCredentials(String dni, String correlationId) {
		return switch (dni) {
			case "00000020" -> success(List.of());
			case "00000021" -> success(List.of(digitalCredential("0000123456", FIRST_DATE,
					"11111111-1111-4111-8111-111111111111")));
			case "00000022" -> success(standardList());
			case "00000023" -> success(List.of(
					digitalCredential("0000123456", FIRST_DATE, "11111111-1111-4111-8111-111111111111"),
					digitalCredential("0000123457", FIRST_DATE.plusSeconds(60), "11111111-1111-4111-8111-111111111111")));
			case "00000024" -> success(List.of(digitalCredential("0000123456", FIRST_DATE, "not-a-uuid")));
			case "00000025" -> failure(Outcome.TIMEOUT, "MOCK_TIMEOUT");
			case "00000026" -> failure(Outcome.UNAVAILABLE, "MOCK_UNAVAILABLE");
			case "00000027" -> failure(Outcome.MALFORMED, "MOCK_MALFORMED_RESPONSE");
			default -> success(standardList());
		};
	}

	private static List<ListedDigitalCredential> standardList() {
		return List.of(
				digitalCredential("0000123456", FIRST_DATE, "11111111-1111-4111-8111-111111111111"),
				digitalCredential("0000123457", Instant.parse("2024-05-12T13:41:00Z"),
						"22222222-2222-4222-8222-222222222222"),
				revokedDigitalCredential("0000123458", Instant.parse("2024-02-28T21:13:00Z"),
						"33333333-3333-4333-8333-333333333333", REVOKED_DATE));
	}

	private static DigitalCredentialListingResult success(List<ListedDigitalCredential> digitalCredentials) {
		return new DigitalCredentialListingResult(Outcome.SUCCESS, digitalCredentials, "mock-digital-credential-list", null);
	}

	private static DigitalCredentialListingResult failure(Outcome outcome, String code) {
		return new DigitalCredentialListingResult(outcome, List.of(), null, code);
	}

	private static ListedDigitalCredential digitalCredential(String order, Instant createdAt, String uuid) {
		return new ListedDigitalCredential(Integer.parseInt(order), "DniPeruanoCredential", createdAt,
				uuid, DigitalCredentialStatus.ACTIVE, null, 0);
	}

	private static ListedDigitalCredential revokedDigitalCredential(String order, Instant createdAt, String uuid,
			Instant revokedAt) {
		return new ListedDigitalCredential(Integer.parseInt(order), "DniPeruanoCredential", createdAt,
				uuid, DigitalCredentialStatus.REVOKED, revokedAt, 1);
	}
}
