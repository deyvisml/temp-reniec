package pe.gob.reniec.credenciales.revocacion.revocation.initiation.antibot;

@FunctionalInterface
public interface AntiBotVerificationPort {

	void verify(String token);
}
