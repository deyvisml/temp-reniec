package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "identity_verification")
public class IdentityVerificationEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private CertificateCancellationRequestEntity request;

	@Column(name = "attempt_number", nullable = false, updatable = false)
	private int attemptNumber;

	@Column(name = "provider", nullable = false, updatable = false, length = 64)
	private String provider;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider_mode", length = 8)
	private IdentityProviderMode providerMode;

	@Enumerated(EnumType.STRING)
	@Column(name = "verification_status", nullable = false, length = 24)
	private IdentityVerificationStatus verificationStatus;

	@Column(name = "external_reference", length = 128)
	private String externalReference;

	@Column(name = "state_hash", length = 64)
	private String stateHash;

	@Column(name = "state_expires_at")
	private Instant stateExpiresAt;

	@Column(name = "state_consumed_at")
	private Instant stateConsumedAt;

	@Column(name = "pkce_verifier_protected", length = 512)
	private String pkceVerifierProtected;

	@Column(name = "provider_session_state", length = 256)
	private String providerSessionState;

	@Column(name = "verified_subject_hash", length = 64)
	private String verifiedSubjectHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "dni_match_result", nullable = false, length = 16)
	private IdentityMatchResult dniMatchResult;

	@Column(name = "started_at", nullable = false, updatable = false)
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "error_or_cancellation_code", length = 64)
	private String errorOrCancellationCode;

	@Column(name = "correlation_id", nullable = false, updatable = false, length = 64)
	private String correlationId;

	@Column(name = "authorization_jti_hash", length = 64)
	private String authorizationJtiHash;

	@Column(name = "authorization_expires_at")
	private Instant authorizationExpiresAt;

	@Column(name = "authorization_invalidated_at")
	private Instant authorizationInvalidatedAt;

	@Column(name = "authorization_invalidation_reason", length = 64)
	private String authorizationInvalidationReason;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected IdentityVerificationEntity() { }

	public IdentityVerificationEntity(CertificateCancellationRequestEntity request, int attemptNumber,
			String provider, Instant startedAt, String correlationId) {
		this(request, attemptNumber, provider, null, startedAt, correlationId);
	}

	public IdentityVerificationEntity(CertificateCancellationRequestEntity request, int attemptNumber,
			String provider, IdentityProviderMode providerMode, Instant startedAt, String correlationId) {
		this.request = Objects.requireNonNull(request, "request");
		if (attemptNumber < 1) throw new IllegalArgumentException("attemptNumber must be positive");
		this.attemptNumber = attemptNumber;
		this.provider = requireText(provider, "provider");
		this.providerMode = providerMode;
		this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
		this.correlationId = requireText(correlationId, "correlationId");
		verificationStatus = IdentityVerificationStatus.STARTED;
		dniMatchResult = IdentityMatchResult.NOT_EVALUATED;
	}

	public void prepareSecurityArtifacts(String stateHash, Instant stateExpiresAt,
			String pkceVerifierProtected) {
		if (verificationStatus != IdentityVerificationStatus.STARTED || this.stateHash != null) {
			throw new IllegalStateException("Identity security artifacts are already prepared");
		}
		this.stateHash = requireLength(stateHash, "stateHash", 64);
		this.stateExpiresAt = Objects.requireNonNull(stateExpiresAt, "stateExpiresAt");
		this.pkceVerifierProtected = requireText(pkceVerifierProtected, "pkceVerifierProtected");
	}

	public void finish(IdentityVerificationStatus status, IdentityMatchResult matchResult,
			Instant completionTime, String externalReference, String errorOrCancellationCode) {
		finish(status, matchResult, completionTime, externalReference, errorOrCancellationCode, null, null);
	}

	public void finish(IdentityVerificationStatus status, IdentityMatchResult matchResult,
			Instant completionTime, String externalReference, String errorOrCancellationCode,
			String providerSessionState, String verifiedSubjectHash) {
		if (status == IdentityVerificationStatus.STARTED) {
			throw new IllegalArgumentException("A completed verification cannot remain STARTED");
		}
		verificationStatus = Objects.requireNonNull(status, "status");
		dniMatchResult = Objects.requireNonNull(matchResult, "matchResult");
		completedAt = Objects.requireNonNull(completionTime, "completionTime");
		this.externalReference = bounded(externalReference, 128);
		this.errorOrCancellationCode = bounded(errorOrCancellationCode, 64);
		this.providerSessionState = bounded(providerSessionState, 256);
		this.verifiedSubjectHash = verifiedSubjectHash == null ? null
				: requireLength(verifiedSubjectHash, "verifiedSubjectHash", 64);
		pkceVerifierProtected = null;
	}

	public void issueAuthorization(String jtiHash, Instant expiresAt) {
		if (verificationStatus != IdentityVerificationStatus.VERIFIED
				|| dniMatchResult != IdentityMatchResult.MATCH) {
			throw new IllegalStateException("Authorization requires a matching verified identity");
		}
		authorizationJtiHash = requireLength(jtiHash, "jtiHash", 64);
		authorizationExpiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
		authorizationInvalidatedAt = null;
		authorizationInvalidationReason = null;
	}

	public void invalidateAuthorization(Instant at, String reason) {
		if (authorizationJtiHash == null) return;
		authorizationInvalidatedAt = Objects.requireNonNull(at, "at");
		authorizationInvalidationReason = bounded(requireText(reason, "reason"), 64);
	}

	@PrePersist
	void initialize() {
		Instant now = Instant.now();
		if (createdAt == null) createdAt = now;
		if (updatedAt == null) updatedAt = now;
		validateDates();
	}

	@PreUpdate
	void updateTimestamp() {
		updatedAt = Instant.now();
		validateDates();
	}

	private void validateDates() {
		if (completedAt != null && completedAt.isBefore(startedAt)) {
			throw new IllegalStateException("Completion cannot precede start");
		}
		if (stateConsumedAt != null && stateExpiresAt == null) {
			throw new IllegalStateException("Consumed state requires expiration");
		}
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
		return value;
	}

	private static String requireLength(String value, String name, int length) {
		String text = requireText(value, name);
		if (text.length() != length) throw new IllegalArgumentException(name + " must contain " + length + " characters");
		return text;
	}

	private static String bounded(String value, int max) {
		if (value == null) return null;
		if (value.length() > max) throw new IllegalArgumentException("value exceeds " + max + " characters");
		return value;
	}

	public Long getId() { return id; }
	public CertificateCancellationRequestEntity getRequest() { return request; }
	public int getAttemptNumber() { return attemptNumber; }
	public String getProvider() { return provider; }
	public IdentityProviderMode getProviderMode() { return providerMode; }
	public IdentityVerificationStatus getVerificationStatus() { return verificationStatus; }
	public String getExternalReference() { return externalReference; }
	public String getStateHash() { return stateHash; }
	public Instant getStateExpiresAt() { return stateExpiresAt; }
	public Instant getStateConsumedAt() { return stateConsumedAt; }
	public String getPkceVerifierProtected() { return pkceVerifierProtected; }
	public String getProviderSessionState() { return providerSessionState; }
	public String getVerifiedSubjectHash() { return verifiedSubjectHash; }
	public IdentityMatchResult getDniMatchResult() { return dniMatchResult; }
	public Instant getStartedAt() { return startedAt; }
	public Instant getCompletedAt() { return completedAt; }
	public String getErrorOrCancellationCode() { return errorOrCancellationCode; }
	public String getCorrelationId() { return correlationId; }
	public String getAuthorizationJtiHash() { return authorizationJtiHash; }
	public Instant getAuthorizationExpiresAt() { return authorizationExpiresAt; }
	public Instant getAuthorizationInvalidatedAt() { return authorizationInvalidatedAt; }
	public String getAuthorizationInvalidationReason() { return authorizationInvalidationReason; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
