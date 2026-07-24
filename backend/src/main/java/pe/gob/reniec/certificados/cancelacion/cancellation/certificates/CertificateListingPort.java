package pe.gob.reniec.certificados.cancelacion.cancellation.certificates;

public interface CertificateListingPort {

	CertificateListingResult listCertificates(String dni, String correlationId);
}
