package pe.gob.reniec.certificados.cancelacion.cancellation.execution;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.revocation", name = "mode", havingValue = "disabled", matchIfMissing = true)
public class DisabledRevocationGateway implements RevocationGateway {
	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public Result revoke(String certificateUuid, String idempotencyKey) {
		throw new IllegalStateException("The institutional revocation service is not configured");
	}
}
