package pe.gob.reniec.credenciales.revocacion.revocation.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class IdentitySecurityArtifacts {

	private final SecureRandom random = new SecureRandom();

	public StateValue newState() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		// OAuth state travels through query strings and form encoding. Base64 URL-safe
		// prevents '+' from being interpreted as a space by compliant form decoders.
		String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		return new StateValue(value, sha256(value));
	}

	public PkceValue newPkce() {
		byte[] bytes = new byte[48];
		random.nextBytes(bytes);
		String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		String challenge = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(digest(verifier.getBytes(StandardCharsets.US_ASCII)));
		return new PkceValue(verifier, challenge);
	}

	public String sha256(String value) {
		return java.util.HexFormat.of().formatHex(digest(value.getBytes(StandardCharsets.UTF_8)));
	}

	private static byte[] digest(byte[] value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 no está disponible", exception);
		}
	}

	public record StateValue(String value, String hash) { }
	public record PkceValue(String verifier, String challenge) { }
}
