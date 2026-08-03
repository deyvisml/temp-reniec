package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class IdentityVerificationEntityTests {

	@Test
	void requiresANameOnlyForSuccessfulVerifications() {
		DigitalCredentialRevocationRequestEntity request =
				new DigitalCredentialRevocationRequestEntity("73905791");
		IdentityVerificationEntity verified = new IdentityVerificationEntity(
				request, 1, "ID_PERU", Instant.now(), "verified-correlation");
		assertThatThrownBy(() -> verified.finish(
				IdentityVerificationStatus.VERIFIED, IdentityMatchResult.MATCH, Instant.now(),
				"external", null, null, null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("verifiedFirstName");

		IdentityVerificationEntity failed = new IdentityVerificationEntity(
				request, 2, "ID_PERU", Instant.now(), "failed-correlation");
		assertThatThrownBy(() -> failed.finish(
				IdentityVerificationStatus.REJECTED, IdentityMatchResult.INCONCLUSIVE, Instant.now(),
				null, "REJECTED", null, null, "ANA"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("only valid");
	}
}
