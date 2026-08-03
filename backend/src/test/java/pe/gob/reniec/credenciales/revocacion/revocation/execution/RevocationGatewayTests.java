package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationResult;

class RevocationGatewayTests {

	@Test
	void normalizesOptionalProviderValues() {
		RevocationGateway.Result result = new RevocationGateway.Result(
				RevocationResult.SUCCEEDED, " external-42 ", " ", Instant.EPOCH);

		assertThat(result.externalReference()).isEqualTo("external-42");
		assertThat(result.errorCode()).isNull();
	}

	@Test
	void rejectsMissingOrUnboundedProviderValues() {
		assertThatThrownBy(() -> new RevocationGateway.Result(
				null, null, null, Instant.EPOCH))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new RevocationGateway.Result(
				RevocationResult.FAILED, "á", null, Instant.EPOCH))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new RevocationGateway.Result(
				RevocationResult.FAILED, null, "E".repeat(65), Instant.EPOCH))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void disabledGatewayFailsClosed() {
		DisabledRevocationGateway gateway = new DisabledRevocationGateway();

		assertThat(gateway.isAvailable()).isFalse();
		assertThatThrownBy(() -> gateway.revoke(new RevocationGateway.Command(
				"11111111-1111-4111-8111-111111111111", 31, "42992664", "key")))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void recoveryThresholdsMustBePositive() {
		RevocationProperties revocation = new RevocationProperties();
		revocation.setStaleSubmissionThreshold(Duration.ZERO);
		ReceiptProperties receipt = new ReceiptProperties();
		receipt.setStaleGenerationThreshold(Duration.ofSeconds(-1));

		assertThatThrownBy(revocation::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("stale-submission-threshold");
		assertThatThrownBy(receipt::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("stale-generation-threshold");
	}
}
