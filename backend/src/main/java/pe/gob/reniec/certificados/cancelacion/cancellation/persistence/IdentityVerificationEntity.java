package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "identity_verification")
public class IdentityVerificationEntity {

	private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");

	@Id @UuidGenerator
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private CertificateCancellationRequestEntity request;

	@Column(name = "attempt_number", nullable = false, updatable = false)
	private int attemptNumber;

	@Column(name = "provider", nullable = false, updatable = false, length = 64)
	private String provider;

	@Enumerated(EnumType.STRING)
	@Column(name = "verification_status", nullable = false, length = 24)
	private IdentityVerificationStatus verificationStatus;

	@Column(name = "external_reference", length = 128)
	private String externalReference;

	@Column(name = "verified_identity_hash", length = 64)
	private String verifiedIdentityHash;

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

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected IdentityVerificationEntity() { }

	public IdentityVerificationEntity(CertificateCancellationRequestEntity request, int attemptNumber,
			String provider, Instant startedAt, String correlationId) {
		this.request = Objects.requireNonNull(request, "request");
		if (attemptNumber < 1) throw new IllegalArgumentException("attemptNumber must be positive");
		this.attemptNumber = attemptNumber;
		this.provider = requireText(provider, "provider");
		this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
		this.correlationId = requireText(correlationId, "correlationId");
		this.verificationStatus = IdentityVerificationStatus.STARTED;
		this.dniMatchResult = IdentityMatchResult.NOT_EVALUATED;
	}

	public void finish(IdentityVerificationStatus status, IdentityMatchResult matchResult,
			Instant completionTime, String externalReference, String verifiedIdentityHash,
			String errorOrCancellationCode) {
		if (status == IdentityVerificationStatus.STARTED) {
			throw new IllegalArgumentException("A completed verification cannot remain STARTED");
		}
		if (verifiedIdentityHash != null && !HASH.matcher(verifiedIdentityHash).matches()) {
			throw new IllegalArgumentException("verifiedIdentityHash has an invalid format");
		}
		this.verificationStatus = Objects.requireNonNull(status, "status");
		this.dniMatchResult = Objects.requireNonNull(matchResult, "matchResult");
		this.completedAt = Objects.requireNonNull(completionTime, "completionTime");
		this.externalReference = externalReference;
		this.verifiedIdentityHash = verifiedIdentityHash;
		this.errorOrCancellationCode = errorOrCancellationCode;
	}

	@PrePersist
	void initialize() {
		if (createdAt == null) createdAt = Instant.now();
		if (completedAt != null && completedAt.isBefore(startedAt)) {
			throw new IllegalStateException("Completion cannot precede start");
		}
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
		return value;
	}

	public UUID getId() { return id; }
	public CertificateCancellationRequestEntity getRequest() { return request; }
	public int getAttemptNumber() { return attemptNumber; }
	public String getProvider() { return provider; }
	public IdentityVerificationStatus getVerificationStatus() { return verificationStatus; }
	public String getExternalReference() { return externalReference; }
	public String getVerifiedIdentityHash() { return verifiedIdentityHash; }
	public IdentityMatchResult getDniMatchResult() { return dniMatchResult; }
	public Instant getStartedAt() { return startedAt; }
	public Instant getCompletedAt() { return completedAt; }
	public String getErrorOrCancellationCode() { return errorOrCancellationCode; }
	public String getCorrelationId() { return correlationId; }
	public Instant getCreatedAt() { return createdAt; }
}
