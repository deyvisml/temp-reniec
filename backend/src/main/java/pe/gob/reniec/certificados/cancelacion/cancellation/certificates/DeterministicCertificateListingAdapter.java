package pe.gob.reniec.certificados.cancelacion.cancellation.certificates;

import java.time.Instant;
import java.util.List;

import pe.gob.reniec.certificados.cancelacion.cancellation.certificates.CertificateListingResult.ListedCertificate;
import pe.gob.reniec.certificados.cancelacion.cancellation.certificates.CertificateListingResult.Outcome;

final class DeterministicCertificateListingAdapter implements CertificateListingPort {

	private static final Instant FIRST_DATE = Instant.parse("2024-07-15T15:24:00Z");

	@Override
	public CertificateListingResult listCertificates(String dni, String correlationId) {
		return switch (dni) {
			case "00000020" -> success(List.of());
			case "00000021" -> success(List.of(certificate("0000123456", FIRST_DATE,
					"11111111-1111-4111-8111-111111111111")));
			case "00000022" -> success(standardList());
			case "00000023" -> success(List.of(
					certificate("0000123456", FIRST_DATE, "11111111-1111-4111-8111-111111111111"),
					certificate("0000123457", FIRST_DATE.plusSeconds(60), "11111111-1111-4111-8111-111111111111")));
			case "00000024" -> success(List.of(certificate("0000123456", FIRST_DATE, "not-a-uuid")));
			case "00000025" -> failure(Outcome.TIMEOUT, "MOCK_TIMEOUT");
			case "00000026" -> failure(Outcome.UNAVAILABLE, "MOCK_UNAVAILABLE");
			case "00000027" -> failure(Outcome.MALFORMED, "MOCK_MALFORMED_RESPONSE");
			default -> success(standardList());
		};
	}

	private static List<ListedCertificate> standardList() {
		return List.of(
				certificate("0000123456", FIRST_DATE, "11111111-1111-4111-8111-111111111111"),
				certificate("0000123457", Instant.parse("2024-05-12T13:41:00Z"),
						"22222222-2222-4222-8222-222222222222"),
				certificate("0000123458", Instant.parse("2024-02-28T21:13:00Z"),
						"33333333-3333-4333-8333-333333333333"));
	}

	private static CertificateListingResult success(List<ListedCertificate> certificates) {
		return new CertificateListingResult(Outcome.SUCCESS, certificates, "mock-certificate-list", null);
	}

	private static CertificateListingResult failure(Outcome outcome, String code) {
		return new CertificateListingResult(outcome, List.of(), null, code);
	}

	private static ListedCertificate certificate(String order, Instant createdAt, String uuid) {
		return new ListedCertificate(order, createdAt, uuid);
	}
}
