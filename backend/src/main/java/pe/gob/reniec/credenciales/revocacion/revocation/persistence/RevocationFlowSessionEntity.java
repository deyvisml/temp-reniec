package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "revocation_flow_session")
public class RevocationFlowSessionEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private DigitalCredentialRevocationRequestEntity request;
	@Enumerated(EnumType.STRING) @Column(name = "session_status", nullable = false, length = 32)
	private FlowSessionStatus status;
	@Column(name = "refresh_family", nullable = false, updatable = false, length = 36)
	private String refreshFamily;
	@Column(name = "refresh_version", nullable = false)
	private int refreshVersion;
	@Column(name = "current_refresh_hash", nullable = false, length = 64)
	private String currentRefreshHash;
	@Column(name = "previous_refresh_hash", length = 64)
	private String previousRefreshHash;
	@Column(name = "previous_valid_until") private Instant previousValidUntil;
	@Column(name = "refresh_expires_at", nullable = false) private Instant refreshExpiresAt;
	@Column(name = "last_used_at", nullable = false) private Instant lastUsedAt;
	@Column(name = "invalidated_at") private Instant invalidatedAt;
	@Column(name = "invalidation_reason", length = 64) private String invalidationReason;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	@Column(name = "updated_at", nullable = false) private Instant updatedAt;
	@Version @Column(name = "version", nullable = false) private long version;

	protected RevocationFlowSessionEntity() { }

	public RevocationFlowSessionEntity(DigitalCredentialRevocationRequestEntity request, String refreshHash,
			Instant expiresAt, Instant now) {
		this.request = Objects.requireNonNull(request);
		this.status = FlowSessionStatus.PENDING_IDENTITY;
		this.refreshFamily = UUID.randomUUID().toString();
		this.refreshVersion = 1;
		this.currentRefreshHash = requireHash(refreshHash);
		this.refreshExpiresAt = Objects.requireNonNull(expiresAt);
		this.lastUsedAt = Objects.requireNonNull(now);
		this.createdAt = now;
		this.updatedAt = now;
	}

	public void rotate(String nextHash, Instant previousUntil, Instant nextRefreshExpiry, Instant now) {
		ensureActive(now);
		previousRefreshHash = currentRefreshHash;
		previousValidUntil = Objects.requireNonNull(previousUntil);
		currentRefreshHash = requireHash(nextHash);
		refreshExpiresAt = Objects.requireNonNull(nextRefreshExpiry);
		refreshVersion++;
		lastUsedAt = now;
		updatedAt = now;
	}
	public void initializeRefreshHash(String hash, Instant now) {
		if (!"0000000000000000000000000000000000000000000000000000000000000000".equals(currentRefreshHash))
			throw new IllegalStateException("Refresh hash is already initialized");
		currentRefreshHash = requireHash(hash);
		updatedAt = now;
	}

	public void markIdentityVerified(Instant now) {
		ensureActive(now);
		status = FlowSessionStatus.IDENTITY_VERIFIED;
		lastUsedAt = now;
		updatedAt = now;
	}

	public void invalidate(String reason, Instant now) {
		if (status == FlowSessionStatus.INVALIDATED || status == FlowSessionStatus.EXPIRED) return;
		status = FlowSessionStatus.INVALIDATED;
		invalidatedAt = now;
		invalidationReason = reason;
		updatedAt = now;
	}
	public void ensureActive(Instant now) {
		if (status == FlowSessionStatus.INVALIDATED || status == FlowSessionStatus.EXPIRED || !refreshExpiresAt.isAfter(now))
			throw new IllegalStateException("Session is not active");
	}
	private static String requireHash(String value) {
		if (value == null || value.length() != 64) throw new IllegalArgumentException("refresh hash must have 64 chars");
		return value;
	}
	public Long getId() { return id; }
	public DigitalCredentialRevocationRequestEntity getRequest() { return request; }
	public FlowSessionStatus getStatus() { return status; }
	public String getRefreshFamily() { return refreshFamily; }
	public int getRefreshVersion() { return refreshVersion; }
	public String getCurrentRefreshHash() { return currentRefreshHash; }
	public String getPreviousRefreshHash() { return previousRefreshHash; }
	public Instant getPreviousValidUntil() { return previousValidUntil; }
	public Instant getRefreshExpiresAt() { return refreshExpiresAt; }
	public Instant getLastUsedAt() { return lastUsedAt; }
	public Instant getInvalidatedAt() { return invalidatedAt; }
	public String getInvalidationReason() { return invalidationReason; }
}
