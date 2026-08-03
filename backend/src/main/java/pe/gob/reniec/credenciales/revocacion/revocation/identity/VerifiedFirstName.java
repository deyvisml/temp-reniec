package pe.gob.reniec.credenciales.revocacion.revocation.identity;

import java.text.Normalizer;

final class VerifiedFirstName {
	private static final int MAX_LENGTH = 100;

	private VerifiedFirstName() { }

	static String normalize(String value) {
		if (value == null) throw invalid();
		String source = Normalizer.normalize(value, Normalizer.Form.NFC);
		StringBuilder normalized = new StringBuilder(source.length());
		boolean pendingSpace = false;
		for (int offset = 0; offset < source.length();) {
			int codePoint = source.codePointAt(offset);
			offset += Character.charCount(codePoint);
			if (Character.isISOControl(codePoint)) throw invalid();
			if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
				pendingSpace = normalized.length() > 0;
				continue;
			}
			if (pendingSpace) normalized.append(' ');
			normalized.appendCodePoint(codePoint);
			pendingSpace = false;
		}
		if (normalized.isEmpty()
				|| normalized.codePointCount(0, normalized.length()) > MAX_LENGTH) {
			throw invalid();
		}
		return normalized.toString();
	}

	private static IdentityIntegrationException invalid() {
		return new IdentityIntegrationException(IdentityFailure.INVALID_RESPONSE,
				"Respuesta inválida de ID Perú");
	}
}
