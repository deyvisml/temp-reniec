package pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials;

public interface DigitalCredentialListingPort {

	DigitalCredentialListingResult listDigitalCredentials(String dni, String correlationId);
}
