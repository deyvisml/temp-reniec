package pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials;

import java.time.Instant;
import java.util.List;

public record DigitalCredentialListingResult(Outcome outcome, List<ListedDigitalCredential> digitalCredentials,
		String externalReference, String errorCode) {

	public DigitalCredentialListingResult {
		digitalCredentials = digitalCredentials == null ? List.of() : List.copyOf(digitalCredentials);
	}

	public enum Outcome { SUCCESS, TIMEOUT, UNAVAILABLE, MALFORMED }

	public record ListedDigitalCredential(int statusListIndex, String credentialType, Instant emissionCreatedAt,
			String digitalCredentialUuid, DigitalCredentialStatus status, Instant revokedAt,
			int providerCredentialStatus) { }
}
