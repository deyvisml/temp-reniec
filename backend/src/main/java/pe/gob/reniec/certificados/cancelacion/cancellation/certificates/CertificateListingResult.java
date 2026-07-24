package pe.gob.reniec.certificados.cancelacion.cancellation.certificates;

import java.time.Instant;
import java.util.List;

public record CertificateListingResult(Outcome outcome, List<ListedCertificate> certificates,
		String externalReference, String errorCode) {

	public CertificateListingResult {
		certificates = certificates == null ? List.of() : List.copyOf(certificates);
	}

	public enum Outcome { SUCCESS, TIMEOUT, UNAVAILABLE, MALFORMED }

	public record ListedCertificate(String orderNumber, Instant emissionCreatedAt, String certificateUuid) { }
}
