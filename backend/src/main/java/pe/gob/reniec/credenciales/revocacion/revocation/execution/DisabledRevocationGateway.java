package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.credential-provider", name = "mode", havingValue = "disabled", matchIfMissing = true)
public class DisabledRevocationGateway implements RevocationGateway {
	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public Result revoke(Command command) {
		throw new IllegalStateException("The institutional revocation service is not configured");
	}
}
