package pe.gob.reniec.credenciales.revocacion.revocation.identity;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityMatchResult;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityProviderMode;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationStatus;

@Service
public class IdentityPersistenceCoordinator {
	private final DigitalCredentialRevocationRequestRepository requests;
	private final IdentityVerificationRepository verifications;

	public IdentityPersistenceCoordinator(ObjectProvider<DigitalCredentialRevocationRequestRepository> requests,
			ObjectProvider<IdentityVerificationRepository> verifications) {
		this.requests = requests.getIfAvailable();
		this.verifications = verifications.getIfAvailable();
	}

	@Transactional
	public PreparedAttempt prepare(Long requestId, IdentityProviderMode mode, String stateHash,
			Instant stateExpiresAt, String protectedVerifier, String correlationId) {
		ensurePersistence();
		DigitalCredentialRevocationRequestEntity request = requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> unauthorized("Solicitud no encontrada"));
		if (request.getRequestStatus() != RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION) {
			throw unauthorized("La solicitud no está habilitada para autenticación");
		}
		Instant now = Instant.now();
		Optional<IdentityVerificationEntity> latest = verifications
				.findTopByRequest_IdOrderByAttemptNumberDesc(requestId);
		latest.filter(current -> current.getVerificationStatus() == IdentityVerificationStatus.STARTED)
				.ifPresent(current -> replaceUnconsumedAttempt(current, now));
		int attempt = latest.map(current -> current.getAttemptNumber() + 1).orElse(1);
		IdentityVerificationEntity entity = new IdentityVerificationEntity(request, attempt, "ID_PERU",
				mode, now, correlationId);
		entity.prepareSecurityArtifacts(stateHash, stateExpiresAt, protectedVerifier);
		entity = verifications.saveAndFlush(entity);
		return new PreparedAttempt(entity.getId(), request.getId(), request.getDni());
	}

	private static void replaceUnconsumedAttempt(IdentityVerificationEntity current, Instant now) {
		if (current.getStateConsumedAt() != null) {
			throw new IdentityIntegrationException(IdentityFailure.IN_PROGRESS,
					"La verificación de identidad está procesando el retorno");
		}
		boolean expired = current.getStateExpiresAt() == null || current.getStateExpiresAt().isBefore(now);
		current.finish(expired ? IdentityVerificationStatus.EXPIRED : IdentityVerificationStatus.CANCELLED,
				IdentityMatchResult.NOT_EVALUATED, now, null,
				expired ? "STATE_EXPIRED" : "REPLACED_BY_NEW_ATTEMPT");
	}

	@Transactional
	public ReservedAttempt reserve(String stateHash, Instant now) {
		ensurePersistence();
		if (verifications.consumeState(stateHash, now) != 1) {
			IdentityVerificationEntity existing = verifications.findByStateHash(stateHash)
					.orElseThrow(() -> new IdentityIntegrationException(IdentityFailure.INVALID_STATE, "State inválido"));
			if (existing.getStateConsumedAt() != null) throw new IdentityIntegrationException(IdentityFailure.CALLBACK_REPLAYED, "Callback repetido");
			throw new IdentityIntegrationException(IdentityFailure.STATE_EXPIRED, "State expirado");
		}
		IdentityVerificationEntity entity = verifications.findByStateHash(stateHash)
				.orElseThrow(() -> new IdentityIntegrationException(IdentityFailure.INVALID_STATE, "State inválido"));
		return new ReservedAttempt(entity.getId(), entity.getRequest().getId(), entity.getRequest().getDni(),
				entity.getPkceVerifierProtected());
	}

	@Transactional
	public void completeSuccess(Long attemptId, String subjectHash, String verifiedFirstName, String externalReference,
			String sessionState) {
		ensurePersistence();
		IdentityVerificationEntity entity = verifications.findByIdForUpdate(attemptId)
				.orElseThrow(() -> unauthorized("Intento no encontrado"));
		entity.finish(IdentityVerificationStatus.VERIFIED, IdentityMatchResult.MATCH, Instant.now(),
				externalReference, null, sessionState, subjectHash, verifiedFirstName);
		DigitalCredentialRevocationRequestEntity request = requests.findByIdForUpdate(entity.getRequest().getId())
				.orElseThrow(() -> unauthorized("Solicitud no encontrada"));
		request.transitionTo(RevocationRequestStatus.IDENTITY_VERIFIED, null);
	}

	@Transactional
	public void completeFailure(Long attemptId, IdentityVerificationStatus status,
			IdentityMatchResult match, String code, String sessionState) {
		ensurePersistence();
		IdentityVerificationEntity entity = verifications.findByIdForUpdate(attemptId)
				.orElseThrow(() -> unauthorized("Intento no encontrado"));
		entity.finish(status, match, Instant.now(), null, code, sessionState, null);
	}

	@Transactional(readOnly = true)
	public Optional<IdentityVerificationEntity> latest(Long requestId) {
		ensurePersistence();
		return verifications.findFirstByRequest_IdOrderByAttemptNumberDesc(requestId);
	}

	private static IdentityIntegrationException unauthorized(String message) {
		return new IdentityIntegrationException(IdentityFailure.UNAUTHORIZED, message);
	}

	private void ensurePersistence() {
		if (requests == null || verifications == null) {
			throw new IdentityIntegrationException(IdentityFailure.UNAVAILABLE, "Persistencia de identidad no disponible");
		}
	}

	public record PreparedAttempt(Long attemptId, Long requestId, String dni) { }
	public record ReservedAttempt(Long attemptId, Long requestId, String dni, String protectedVerifier) { }
}
