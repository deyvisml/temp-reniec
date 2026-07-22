package pe.gob.reniec.certificados.cancelacion.cancellation.initiation.antibot;

@FunctionalInterface
public interface AntiBotVerificationPort {

	void verify(String token);
}
