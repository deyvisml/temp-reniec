package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class TransientSecretProtector {

	private static final int IV_LENGTH = 12;
	private static final int TAG_BITS = 128;
	private final byte[] key;
	private final SecureRandom random = new SecureRandom();

	public TransientSecretProtector(IdPeruFlowKeys keys) {
		this.key = keys.pkceEncryptionKey();
	}

	public String protect(String plaintext) {
		try {
			byte[] iv = new byte[IV_LENGTH];
			random.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
			byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding()
					.encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
		}
		catch (GeneralSecurityException exception) {
			throw new IdentityIntegrationException(IdentityFailure.CONFIGURATION, "No se pudo proteger PKCE", exception);
		}
	}

	public String reveal(String protectedValue) {
		try {
			byte[] packed = Base64.getUrlDecoder().decode(protectedValue);
			if (packed.length <= IV_LENGTH) throw new GeneralSecurityException("invalid protected value");
			byte[] iv = java.util.Arrays.copyOfRange(packed, 0, IV_LENGTH);
			byte[] encrypted = java.util.Arrays.copyOfRange(packed, IV_LENGTH, packed.length);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		}
		catch (GeneralSecurityException | IllegalArgumentException exception) {
			throw new IdentityIntegrationException(IdentityFailure.INVALID_CALLBACK, "PKCE protegido no es válido", exception);
		}
	}
}
