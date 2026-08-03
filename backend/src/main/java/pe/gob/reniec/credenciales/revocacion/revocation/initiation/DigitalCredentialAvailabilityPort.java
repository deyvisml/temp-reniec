package pe.gob.reniec.credenciales.revocacion.revocation.initiation;

public interface DigitalCredentialAvailabilityPort {

	AvailabilityResult checkAvailability(String dni);
}
