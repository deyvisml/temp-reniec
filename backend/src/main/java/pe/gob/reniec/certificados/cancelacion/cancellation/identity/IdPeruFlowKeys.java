package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class IdPeruFlowKeys {

	private static final byte[] PKCE_CONTEXT = "id-peru/pkce-encryption/v1".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] SIGNING_CONTEXT = "id-peru/flow-signing/v1".getBytes(StandardCharsets.US_ASCII);
	private final byte[] masterSecret;

	public IdPeruFlowKeys(IdPeruProperties properties) {
		this.masterSecret = properties.flowSecretBytes();
	}

	byte[] pkceEncryptionKey() {
		return derive(PKCE_CONTEXT);
	}

	byte[] flowSigningKey() {
		return derive(SIGNING_CONTEXT);
	}

	private byte[] derive(byte[] context) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(masterSecret, "HmacSHA256"));
			return mac.doFinal(context);
		}
		catch (java.security.GeneralSecurityException exception) {
			throw new IllegalStateException("No se pudieron derivar las claves del flujo de ID Perú", exception);
		}
	}
}
