package pe.gob.reniec.credenciales.revocacion.revocation.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Service
public class FlowSessionService {
	private static final String EMPTY_HASH = "0".repeat(64);
	private final RevocationFlowSessionRepository sessions;
	private final DigitalCredentialRevocationRequestRepository requests;
	private final FlowSessionJwtService jwt;
	private final FlowSessionProperties properties;

	public FlowSessionService(ObjectProvider<RevocationFlowSessionRepository> sessions,
			ObjectProvider<DigitalCredentialRevocationRequestRepository> requests, FlowSessionJwtService jwt,
			FlowSessionProperties properties) {
		this.sessions = sessions.getIfAvailable(); this.requests = requests.getIfAvailable(); this.jwt = jwt; this.properties = properties;
	}

	@Transactional
	public Tokens establish(Long requestId) {
		ensurePersistence();
		Instant now = Instant.now();
		DigitalCredentialRevocationRequestEntity request = requests.findByIdForUpdate(requestId)
				.orElseThrow(() -> new FlowSessionException(FlowSessionException.Reason.INVALID));
		if (request.getRequestStatus() != RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION)
			throw new FlowSessionException(FlowSessionException.Reason.FORBIDDEN);
		if (sessions.findByRequest_Id(requestId).isPresent())
			throw new FlowSessionException(FlowSessionException.Reason.ALREADY_ACTIVE);
		RevocationFlowSessionEntity session = sessions.saveAndFlush(new RevocationFlowSessionEntity(request,
				EMPTY_HASH, now.plus(properties.getRefreshTtl()), now));
		FlowSessionJwtService.IssuedToken refresh = jwt.issueRefresh(session.getId(), requestId,
				session.getRefreshFamily(), session.getRefreshVersion());
		session.initializeRefreshHash(hash(refresh.value()), now);
		FlowSessionJwtService.IssuedToken access = jwt.issueAccess(session.getId(), requestId);
		return new Tokens(access, refresh);
	}

	@Transactional(readOnly = true)
	public CurrentSession current(String accessToken) {
		ensurePersistence();
		FlowSessionJwtService.Claims claims = jwt.validate(accessToken, "access");
		RevocationFlowSessionEntity session = sessions.findById(claims.sessionId())
				.orElseThrow(() -> invalid(FlowSessionException.Reason.INVALID));
		validateBinding(session, claims.requestId());
		return describe(session);
	}

	@Transactional(readOnly = true)
	public Long requireRequest(String accessToken) { return current(accessToken).requestId(); }

	@Transactional(noRollbackFor = FlowSessionException.class)
	public Tokens refresh(String refreshToken) {
		ensurePersistence();
		FlowSessionJwtService.Claims claims = jwt.validate(refreshToken, "refresh");
		RevocationFlowSessionEntity session = sessions.findByIdForUpdate(claims.sessionId())
				.orElseThrow(() -> invalid(FlowSessionException.Reason.INVALID));
		Instant now = Instant.now();
		validateBinding(session, claims.requestId());
		String presented = hash(refreshToken);
		if (!session.getRefreshFamily().equals(claims.family())) throw invalid(FlowSessionException.Reason.INVALID);
		if (claims.version() != null && session.getCurrentRefreshHash().equals(presented)
				&& session.getRefreshVersion() == claims.version()) {
			FlowSessionJwtService.IssuedToken next = jwt.issueRefresh(session.getId(), claims.requestId(),
					session.getRefreshFamily(), session.getRefreshVersion() + 1);
			session.rotate(hash(next.value()), now.plus(properties.getConcurrentRefreshWindow()),
					next.expiresAt(), now);
			return new Tokens(jwt.issueAccess(session.getId(), claims.requestId()), next);
		}
		if (presented.equals(session.getPreviousRefreshHash()) && session.getPreviousValidUntil() != null
				&& session.getPreviousValidUntil().isAfter(now)) {
			throw invalid(FlowSessionException.Reason.REFRESH_CONFLICT);
		}
		session.invalidate("REFRESH_REPLAY", now);
		throw invalid(FlowSessionException.Reason.REPLAYED);
	}

	@Transactional
	public FlowSessionJwtService.IssuedToken markIdentityVerified(Long requestId) {
		ensurePersistence();
		RevocationFlowSessionEntity session = sessions.findByRequest_Id(requestId)
				.orElseThrow(() -> invalid(FlowSessionException.Reason.INVALID));
		session.markIdentityVerified(Instant.now());
		return jwt.issueAccess(session.getId(), requestId);
	}

	@Transactional
	public void logout(String accessToken) {
		ensurePersistence();
		FlowSessionJwtService.Claims claims = jwt.validate(accessToken, "access");
		RevocationFlowSessionEntity session = sessions.findByIdForUpdate(claims.sessionId())
				.orElseThrow(() -> invalid(FlowSessionException.Reason.INVALID));
		validateBinding(session, claims.requestId());
		Instant now = Instant.now();
		session.invalidate("LOGOUT", now);
		DigitalCredentialRevocationRequestEntity request = session.getRequest();
		if (isAbandonable(request.getRequestStatus())) request.transitionTo(RevocationRequestStatus.ABANDONED, null);
	}

	private CurrentSession describe(RevocationFlowSessionEntity session) {
		DigitalCredentialRevocationRequestEntity request = session.getRequest();
		String next = switch (request.getRequestStatus()) {
			case IDENTITY_VERIFIED, AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST, CHECKING_DIGITAL_CREDENTIAL_LIST,
					NO_DIGITAL_CREDENTIALS_AVAILABLE,
					DIGITAL_CREDENTIALS_AVAILABLE, DIGITAL_CREDENTIALS_SELECTED, REASON_REGISTERED,
					PENDING_CONFIRMATION -> "DIGITAL_CREDENTIAL_SELECTION";
			case CONFIRMED, REVOCATION_IN_PROGRESS, REVOCATION_FAILED, REVOCATION_OUTCOME_UNKNOWN,
					FAILED, OUTCOME_UNKNOWN, REVOCATION_SUCCEEDED, COMPLETED -> "CONFIRMATION";
			case RECEIPT_AVAILABLE -> "RECEIPT";
			default -> "IDENTITY_VERIFICATION";
		};
		return new CurrentSession(session.getId(), request.getId(), request.getDni(),
				session.getStatus().name(), request.getRequestStatus().name(), next);
	}
	private void validateBinding(RevocationFlowSessionEntity session, Long requestId) {
		if (!session.getRequest().getId().equals(requestId)) throw invalid(FlowSessionException.Reason.INVALID);
		try { session.ensureActive(Instant.now()); }
		catch (IllegalStateException ex) { throw invalid(FlowSessionException.Reason.EXPIRED); }
	}
	private static boolean isAbandonable(RevocationRequestStatus status) {
		return switch (status) {
			case PENDING_IDENTITY_VERIFICATION, IDENTITY_VERIFIED, AUTHENTICATED_PENDING_DIGITAL_CREDENTIAL_LIST,
					CHECKING_DIGITAL_CREDENTIAL_LIST,
					DIGITAL_CREDENTIALS_AVAILABLE, DIGITAL_CREDENTIALS_SELECTED, REASON_REGISTERED, PENDING_CONFIRMATION -> true;
			default -> false;
		};
	}
	private static String hash(String value) {
		try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
		catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
	}
	private static FlowSessionException invalid(FlowSessionException.Reason reason) { return new FlowSessionException(reason); }
	private void ensurePersistence() {
		if (sessions == null || requests == null) throw invalid(FlowSessionException.Reason.INVALID);
	}
	public record Tokens(FlowSessionJwtService.IssuedToken access, FlowSessionJwtService.IssuedToken refresh) { }
	@Schema(description = "Contexto seguro de la operación activa y el siguiente paso autorizado.")
	public record CurrentSession(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Identificador técnico de sesión.") Long sessionId,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Identificador técnico de solicitud.") Long requestId,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^[0-9]{8}$", description = "DNI completo mostrado únicamente dentro de la sesión autenticada.") String dni,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"PENDING_IDENTITY", "IDENTITY_VERIFIED"}) String sessionStatus,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Estado controlado de la solicitud.") String requestStatus,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"IDENTITY_VERIFICATION", "DIGITAL_CREDENTIAL_SELECTION", "CONFIRMATION", "RECEIPT"}) String nextStep) { }
}
