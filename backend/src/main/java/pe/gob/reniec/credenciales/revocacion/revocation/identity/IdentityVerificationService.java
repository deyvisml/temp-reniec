package pe.gob.reniec.credenciales.revocacion.revocation.identity;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityMatchResult;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityProviderMode;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.session.FlowSessionJwtService;
import pe.gob.reniec.credenciales.revocacion.revocation.session.FlowSessionService;

@Service
public class IdentityVerificationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(IdentityVerificationService.class);
	private static final Pattern STATE_VALUE = Pattern.compile("[A-Za-z0-9_-]{1,512}");
	private static final Pattern CODE_VALUE = Pattern.compile("[A-Za-z0-9._-]{1,512}");
	private static final Pattern SESSION_STATE_VALUE = Pattern.compile("[A-Za-z0-9._-]{1,256}");
	private static final Pattern PROVIDER_ERROR_VALUE = Pattern.compile("[A-Za-z0-9._-]{1,64}");
	private final IdPeruProperties properties;
	private final FlowSessionService sessions;
	private final IdentitySecurityArtifacts security;
	private final TransientSecretProtector protector;
	private final CitizenIdentityProviderPort provider;
	private final IdentityPersistenceCoordinator persistence;

	public IdentityVerificationService(IdPeruProperties properties, FlowSessionService sessions,
			IdentitySecurityArtifacts security, TransientSecretProtector protector,
			CitizenIdentityProviderPort provider, IdentityPersistenceCoordinator persistence) {
		this.properties = properties; this.sessions = sessions; this.security = security;
		this.protector = protector; this.provider = provider; this.persistence = persistence;
	}

	public URI start(String continuityToken, String correlationId) {
		Long requestId = sessions.requireRequest(continuityToken);
		IdentitySecurityArtifacts.StateValue state = security.newState();
		IdentitySecurityArtifacts.PkceValue pkce = security.newPkce();
		IdentityPersistenceCoordinator.PreparedAttempt attempt = persistence.prepare(requestId,
				properties.getMode() == IdPeruMode.REAL ? IdentityProviderMode.REAL : IdentityProviderMode.MOCK,
				state.hash(), Instant.now().plus(properties.getStateTtl()), protector.protect(pkce.verifier()), correlationId);
		return provider.authorizationUri(new CitizenIdentityProviderPort.AuthorizationContext(
				state.value(), pkce.challenge(), attempt.dni()));
	}

	public CallbackResult callback(String code, String state, String sessionState, String providerError) {
		long startedAt = System.nanoTime();
		LOGGER.info("ID Peru callback received codePresent={} statePresent={} sessionStatePresent={} providerErrorPresent={}",
				present(code), present(state), present(sessionState), present(providerError));
		if (!matches(STATE_VALUE, state)) {
			throw new IdentityIntegrationException(IdentityFailure.INVALID_STATE, "State inválido");
		}
		IdentityPersistenceCoordinator.ReservedAttempt attempt = persistence.reserve(security.sha256(state), Instant.now());
		if (!matchesOptional(SESSION_STATE_VALUE, sessionState)
				|| !matchesOptional(PROVIDER_ERROR_VALUE, providerError)) {
			persistence.completeFailure(attempt.attemptId(), IdentityVerificationStatus.ERROR,
					IdentityMatchResult.NOT_EVALUATED, "INVALID_CALLBACK", null);
			logCallbackResult("ERROR", "CALLBACK", "INVALID_CALLBACK", attempt.attemptId(), startedAt);
			return new CallbackResult(false, null, "ERROR");
		}
		if (providerError != null && !providerError.isBlank()) {
			IdentityVerificationStatus status = "access_denied".equals(providerError)
					? IdentityVerificationStatus.CANCELLED : IdentityVerificationStatus.REJECTED;
			persistence.completeFailure(attempt.attemptId(), status, IdentityMatchResult.NOT_EVALUATED,
					providerError, sessionState);
			logCallbackResult(status.name(), "CALLBACK", "PROVIDER_" + status.name(), attempt.attemptId(), startedAt);
			return new CallbackResult(false, null, status.name());
		}
		if (!matches(CODE_VALUE, code)
				|| properties.requiresSessionState()
				&& !matches(SESSION_STATE_VALUE, sessionState)) {
			persistence.completeFailure(attempt.attemptId(), IdentityVerificationStatus.ERROR,
					IdentityMatchResult.NOT_EVALUATED, "MISSING_CODE", sessionState);
			logCallbackResult("ERROR", "CALLBACK", "MISSING_CODE", attempt.attemptId(), startedAt);
			return new CallbackResult(false, null, "ERROR");
		}
		try {
			CitizenIdentityProviderPort.VerifiedCitizen citizen = provider.authenticate(code, sessionState,
					protector.reveal(attempt.protectedVerifier()), attempt.dni());
			if (!Objects.equals(attempt.dni(), citizen.dni())) {
				persistence.completeFailure(attempt.attemptId(), IdentityVerificationStatus.IDENTITY_MISMATCH,
						IdentityMatchResult.MISMATCH, "IDENTITY_MISMATCH", sessionState);
				logCallbackResult("IDENTITY_MISMATCH", "VALIDATION", "IDENTITY_MISMATCH",
						attempt.attemptId(), startedAt);
				return new CallbackResult(false, null, "IDENTITY_MISMATCH");
			}
			persistence.completeSuccess(attempt.attemptId(), security.sha256(citizen.subject()),
					VerifiedFirstName.normalize(citizen.firstName()), citizen.externalReference(), sessionState);
			FlowSessionJwtService.IssuedToken access = sessions.markIdentityVerified(attempt.requestId());
			logCallbackResult("VERIFIED", "COMPLETION", null, attempt.attemptId(), startedAt);
			return new CallbackResult(true, access, "VERIFIED");
		}
		catch (IdentityIntegrationException exception) {
			persistence.completeFailure(attempt.attemptId(), mapStatus(exception.failure()),
					IdentityMatchResult.INCONCLUSIVE, exception.technicalCode(), sessionState);
			logCallbackResult(exception.failure().name(), diagnosticPhase(exception.technicalCode()),
					exception.technicalCode(), attempt.attemptId(), startedAt);
			return new CallbackResult(false, null, exception.failure().name());
		}
	}

	private static void logCallbackResult(String outcome, String phase, String technicalCode,
			Long attemptId, long startedAt) {
		String template = "ID Peru callback completed outcome={} phase={} technicalCode={} attemptId={} durationMs={}";
		long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
		if ("VERIFIED".equals(outcome) || "CANCELLED".equals(outcome) || "REJECTED".equals(outcome)) {
			LOGGER.info(template, outcome, phase, technicalCode, attemptId, durationMs);
		}
		else {
			LOGGER.warn(template, outcome, phase, technicalCode, attemptId, durationMs);
		}
	}

	private static String diagnosticPhase(String technicalCode) {
		if (technicalCode == null || technicalCode.isBlank()) return "UNKNOWN";
		for (String phase : new String[] { "TOKEN", "USERINFO", "JWKS", "VALIDATION", "PERSISTENCE", "CALLBACK" }) {
			if (technicalCode.startsWith(phase)) return phase;
		}
		return "AUTHENTICATION";
	}

	private static boolean present(String value) {
		return value != null && !value.isBlank();
	}

	private static boolean matches(Pattern pattern, String value) {
		return value != null && pattern.matcher(value).matches();
	}

	private static boolean matchesOptional(Pattern pattern, String value) {
		return value == null || pattern.matcher(value).matches();
	}

	public CurrentIdentityStatus current(String cookie) {
		Long requestId = sessions.requireRequest(cookie);
		IdentityVerificationEntity latest = persistence.latest(requestId).orElse(null);
		if (latest == null) {
			return new CurrentIdentityStatus(IdentityVerificationStatus.STARTED.name(), false,
					"IDENTITY_VERIFICATION");
		}
		boolean authorized = latest.getVerificationStatus() == IdentityVerificationStatus.VERIFIED
				&& latest.getVerifiedFirstName() != null
				&& !latest.getVerifiedFirstName().isBlank()
				&& isActiveFlowStatus(latest.getRequest().getRequestStatus());
		return new CurrentIdentityStatus(latest.getVerificationStatus().name(), authorized,
				authorized ? "DIGITAL_CREDENTIAL_SELECTION" : "IDENTITY_VERIFICATION");
	}

	private static boolean isActiveFlowStatus(RevocationRequestStatus status) {
		return switch (status) {
			case NO_DIGITAL_CREDENTIALS_AVAILABLE, REVOCATION_SUCCEEDED, REVOCATION_FAILED,
					REVOCATION_OUTCOME_UNKNOWN, COMPLETED, FAILED, OUTCOME_UNKNOWN,
					RECEIPT_AVAILABLE, ABANDONED -> false;
			default -> true;
		};
	}

	private static IdentityVerificationStatus mapStatus(IdentityFailure failure) {
		return switch (failure) {
			case CANCELLED -> IdentityVerificationStatus.CANCELLED;
			case REJECTED, TOKEN_REJECTED -> IdentityVerificationStatus.REJECTED;
			case STATE_EXPIRED -> IdentityVerificationStatus.EXPIRED;
			case IDENTITY_MISMATCH -> IdentityVerificationStatus.IDENTITY_MISMATCH;
			default -> IdentityVerificationStatus.ERROR;
		};
	}

	public record CallbackResult(boolean verified, FlowSessionJwtService.IssuedToken accessToken, String status) { }
	public record CurrentIdentityStatus(String status, boolean canContinue, String nextStep) { }
}
