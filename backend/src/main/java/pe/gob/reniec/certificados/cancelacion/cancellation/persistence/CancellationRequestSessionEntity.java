package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "cancellation_request_session")
public class CancellationRequestSessionEntity {

	private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");

	@Id @UuidGenerator
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private CertificateCancellationRequestEntity request;

	@Column(name = "session_reference_hash", nullable = false, unique = true, updatable = false, length = 64)
	private String sessionReferenceHash;

	@Column(name = "token_family_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID tokenFamilyId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "last_used_at")
	private Instant lastUsedAt;

	@Column(name = "invalidated_at")
	private Instant invalidatedAt;

	@Column(name = "invalidation_reason", length = 64)
	private String invalidationReason;

	@Column(name = "client_reference_hash", length = 64)
	private String clientReferenceHash;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected CancellationRequestSessionEntity() { }

	public CancellationRequestSessionEntity(CertificateCancellationRequestEntity request,
			String sessionReferenceHash, UUID tokenFamilyId, Instant expiresAt, String clientReferenceHash) {
		this.request = Objects.requireNonNull(request, "request");
		this.sessionReferenceHash = requireHash(sessionReferenceHash, "sessionReferenceHash");
		this.tokenFamilyId = Objects.requireNonNull(tokenFamilyId, "tokenFamilyId");
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
		this.clientReferenceHash = clientReferenceHash == null ? null
				: requireHash(clientReferenceHash, "clientReferenceHash");
	}

	public void touch(Instant at) { lastUsedAt = Objects.requireNonNull(at, "at"); }

	public void invalidate(Instant at, String reason) {
		invalidatedAt = Objects.requireNonNull(at, "at");
		if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason must not be blank");
		invalidationReason = reason;
	}

	@PrePersist
	void initialize() {
		Instant now = Instant.now();
		if (createdAt == null) createdAt = now;
		updatedAt = now;
		validateTimes();
	}

	@PreUpdate
	void update() { updatedAt = Instant.now(); validateTimes(); }

	private void validateTimes() {
		if (!expiresAt.isAfter(createdAt)) throw new IllegalStateException("Session expiry must be after creation");
		if (lastUsedAt != null && lastUsedAt.isBefore(createdAt)) throw new IllegalStateException("Last use cannot precede creation");
		if ((invalidatedAt == null) != (invalidationReason == null)) throw new IllegalStateException("Invalidation time and reason must be stored together");
		if (invalidatedAt != null && invalidatedAt.isBefore(createdAt)) throw new IllegalStateException("Invalidation cannot precede creation");
	}

	private static String requireHash(String value, String name) {
		if (value == null || !HASH.matcher(value).matches()) throw new IllegalArgumentException(name + " has an invalid format");
		return value;
	}

	public UUID getId() { return id; }
	public CertificateCancellationRequestEntity getRequest() { return request; }
	public String getSessionReferenceHash() { return sessionReferenceHash; }
	public UUID getTokenFamilyId() { return tokenFamilyId; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getExpiresAt() { return expiresAt; }
	public Instant getLastUsedAt() { return lastUsedAt; }
	public Instant getInvalidatedAt() { return invalidatedAt; }
	public String getInvalidationReason() { return invalidationReason; }
	public String getClientReferenceHash() { return clientReferenceHash; }
	public Instant getUpdatedAt() { return updatedAt; }
}
