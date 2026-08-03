package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationResult;

@Component
@ConditionalOnProperty(prefix = "app.credential-provider", name = "mode", havingValue = "mock")
public class MockRevocationGateway implements RevocationGateway {
	private final RevocationProperties properties;

	public MockRevocationGateway(RevocationProperties properties) { this.properties = properties; }

	@Override
	public Result revoke(Command command) {
		RevocationResult outcome = properties.getMockOutcome();
		String error = outcome == RevocationResult.FAILED ? "MOCK_REJECTED"
				: outcome == RevocationResult.OUTCOME_UNKNOWN ? "MOCK_OUTCOME_UNKNOWN" : null;
		Integer providerStatus = outcome == RevocationResult.SUCCEEDED ? 1
				: outcome == RevocationResult.FAILED ? 0 : null;
		return new Result(outcome, "mock-" + command.idempotencyKey(), error, Instant.now(), providerStatus);
	}
}
