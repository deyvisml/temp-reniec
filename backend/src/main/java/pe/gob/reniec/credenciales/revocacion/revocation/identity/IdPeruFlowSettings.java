package pe.gob.reniec.credenciales.revocacion.revocation.identity;

import java.time.Duration;

final class IdPeruFlowSettings {

	static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
	static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
	static final Duration STATE_TTL = Duration.ofMinutes(5);
	static final Duration JWKS_TTL = Duration.ofMinutes(15);
	static final String ACR_VALUES = "face_mobile";
	static final String CALLBACK_PATH = "/api/v1/idperu/callback";
	static final String FRONTEND_RETURN_PATH = "/revocacion";

	private IdPeruFlowSettings() {
	}
}
