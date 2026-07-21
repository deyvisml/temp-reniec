package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

public interface CertificateAvailabilityPort {

	AvailabilityResult checkAvailability(String dni);
}
