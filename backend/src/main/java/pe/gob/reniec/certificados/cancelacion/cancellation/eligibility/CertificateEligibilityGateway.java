package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

public interface CertificateEligibilityGateway {

	EligibilityGatewayResult check(String dni);
}
