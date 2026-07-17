package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "cancellation_request_session")
public class CancellationRequestSessionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private CertificateCancellationRequestEntity request;

	@Column(name = "session_reference", nullable = false, unique = true, updatable = false, length = 64)
	private String sessionReference;

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

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected CancellationRequestSessionEntity() { }

	public CancellationRequestSessionEntity(CertificateCancellationRequestEntity request,
			String sessionReference, Instant expiresAt) {
		this.request = Objects.requireNonNull(request, "request");
		this.sessionReference = requireReference(sessionReference);
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
	}

	public void touch(Instant at) { lastUsedAt = Objects.requireNonNull(at, "at"); }

	public void invalidate(Instant at, String reason) {
		invalidatedAt = Objects.requireNonNull(at, "at");
		invalidationReason = requireText(reason, "reason");
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

	private static String requireReference(String value) {
		String reference = requireText(value, "sessionReference");
		if (reference.length() > 64) throw new IllegalArgumentException("sessionReference is too long");
		return reference;
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
		return value;
	}

	public Long getId() { return id; }
	public CertificateCancellationRequestEntity getRequest() { return request; }
	public String getSessionReference() { return sessionReference; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getExpiresAt() { return expiresAt; }
	public Instant getLastUsedAt() { return lastUsedAt; }
	public Instant getInvalidatedAt() { return invalidatedAt; }
	public String getInvalidationReason() { return invalidationReason; }
	public Instant getUpdatedAt() { return updatedAt; }
}
