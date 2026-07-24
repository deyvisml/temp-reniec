package pe.gob.reniec.certificados.cancelacion.cancellation.certificates;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.gob.reniec.certificados.cancelacion.cancellation.certificates.CertificateListingResult.Outcome;

@Configuration
class CertificateListingConfiguration {

	@Bean
	CertificateListingPort certificateListingPort(CertificateListingProperties properties) {
		if (properties.getMode() == CertificateListingProperties.Mode.MOCK) {
			return new DeterministicCertificateListingAdapter();
		}
		return (dni, correlationId) -> new CertificateListingResult(Outcome.UNAVAILABLE,
				List.of(), null, "CERTIFICATE_LISTING_DISABLED");
	}
}
