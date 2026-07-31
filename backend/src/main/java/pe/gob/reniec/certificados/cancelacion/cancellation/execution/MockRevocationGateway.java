package pe.gob.reniec.certificados.cancelacion.cancellation.execution;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationResult;

@Component
@ConditionalOnProperty(prefix = "app.revocation", name = "mode", havingValue = "mock")
public class MockRevocationGateway implements RevocationGateway {
	private final RevocationProperties properties;

	public MockRevocationGateway(RevocationProperties properties) { this.properties = properties; }

	@Override
	public Result revoke(String certificateUuid, String idempotencyKey) {
		RevocationResult outcome = properties.getMockOutcome();
		String error = outcome == RevocationResult.FAILED ? "MOCK_REJECTED"
				: outcome == RevocationResult.OUTCOME_UNKNOWN ? "MOCK_OUTCOME_UNKNOWN" : null;
		return new Result(outcome, "mock-" + idempotencyKey, error, Instant.now());
	}
}
