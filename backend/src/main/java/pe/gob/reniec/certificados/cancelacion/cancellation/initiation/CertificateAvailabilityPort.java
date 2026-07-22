package pe.gob.reniec.certificados.cancelacion.cancellation.initiation;

public interface CertificateAvailabilityPort {

	AvailabilityResult checkAvailability(String dni);
}
