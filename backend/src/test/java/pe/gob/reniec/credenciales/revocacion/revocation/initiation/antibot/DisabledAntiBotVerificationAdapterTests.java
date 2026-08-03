package pe.gob.reniec.credenciales.revocacion.revocation.initiation.antibot;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class DisabledAntiBotVerificationAdapterTests {

	@Test
	void acceptsRequestsWithoutAntiBotEvidence() {
		DisabledAntiBotVerificationAdapter adapter = new DisabledAntiBotVerificationAdapter();

		assertThatCode(() -> adapter.verify(null)).doesNotThrowAnyException();
		assertThatCode(() -> adapter.verify("")).doesNotThrowAnyException();
	}
}
