package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class IdPeruDniEncryptor {

	public String encrypt(String dni, String clientId) {
		if (clientId == null || clientId.length() < 16) {
			throw new IdentityIntegrationException(IdentityFailure.CONFIGURATION, "client_id no es válido");
		}
		try {
			byte[] keyAndIv = clientId.substring(0, 16).getBytes(StandardCharsets.UTF_8);
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyAndIv, "AES"), new IvParameterSpec(keyAndIv));
			return Base64.getEncoder().encodeToString(cipher.doFinal(dni.getBytes(StandardCharsets.UTF_8)));
		}
		catch (GeneralSecurityException exception) {
			throw new IdentityIntegrationException(IdentityFailure.CONFIGURATION, "No se pudo cifrar vd", exception);
		}
	}
}
