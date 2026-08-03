package pe.gob.reniec.credenciales.revocacion.revocation.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VerifiedFirstNameTests {

	@Test
	void normalizesUnicodeAndRepeatedSpacing() {
		assertThat(VerifiedFirstName.normalize("  Jose\u0301\u2003  Luis  "))
				.isEqualTo("José Luis");
	}

	@Test
	void rejectsMissingControlOrOversizedValues() {
		assertInvalid(null);
		assertInvalid("   ");
		assertInvalid("ANA\nMARÍA");
		assertInvalid("A".repeat(101));
	}

	private static void assertInvalid(String value) {
		assertThatThrownBy(() -> VerifiedFirstName.normalize(value))
				.isInstanceOfSatisfying(IdentityIntegrationException.class,
						exception -> assertThat(exception.failure())
							.isEqualTo(IdentityFailure.INVALID_RESPONSE));
	}
}
