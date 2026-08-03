package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

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
@Table(name = "revocation_operation")
public class RevocationOperationEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private DigitalCredentialRevocationRequestEntity request;

	@Column(name = "idempotency_key", nullable = false, unique = true, updatable = false, length = 64)
	private String idempotencyKey;

	@Column(name = "attempt_number", nullable = false, updatable = false)
	private int attemptNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "operation_status", nullable = false, length = 24)
	private RevocationOperationStatus operationStatus;

	@Column(name = "external_reference", length = 128)
	private String externalReference;

	@Column(name = "prepared_at", nullable = false, updatable = false)
	private Instant preparedAt;

	@Column(name = "submitted_at")
	private Instant submittedAt;

	@Column(name = "responded_at")
	private Instant respondedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "normalized_result", length = 40)
	private RevocationResult normalizedResult;

	@Column(name = "error_code", length = 64)
	private String errorCode;

	@Column(name = "provider_credential_status")
	private Integer providerCredentialStatus;

	@Column(name = "correlation_id", nullable = false, updatable = false, length = 64)
	private String correlationId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected RevocationOperationEntity() { }

	public RevocationOperationEntity(DigitalCredentialRevocationRequestEntity request, String idempotencyKey,
			int attemptNumber, Instant preparedAt, String correlationId) {
		this.request = Objects.requireNonNull(request, "request");
		this.idempotencyKey = requireBoundedText(idempotencyKey, "idempotencyKey", 64);
		if (attemptNumber < 1) throw new IllegalArgumentException("attemptNumber must be positive");
		this.attemptNumber = attemptNumber;
		this.preparedAt = Objects.requireNonNull(preparedAt, "preparedAt");
		this.correlationId = requireBoundedText(correlationId, "correlationId", 64);
		operationStatus = RevocationOperationStatus.PREPARED;
	}

	public void markSubmitted(Instant at, String externalReference) {
		operationStatus = RevocationOperationStatus.SUBMITTED;
		submittedAt = Objects.requireNonNull(at, "at");
		this.externalReference = externalReference;
	}

	public void complete(RevocationOperationStatus status, RevocationResult result, Instant responseTime,
			Instant completionTime, String externalReference, String errorCode) {
		complete(status, result, responseTime, completionTime, externalReference, errorCode, null);
	}

	public void complete(RevocationOperationStatus status, RevocationResult result, Instant responseTime,
			Instant completionTime, String externalReference, String errorCode, Integer providerCredentialStatus) {
		if (status == RevocationOperationStatus.PREPARED || status == RevocationOperationStatus.SUBMITTED) {
			throw new IllegalArgumentException("Completion requires a terminal or uncertain status");
		}
		operationStatus = Objects.requireNonNull(status, "status");
		normalizedResult = Objects.requireNonNull(result, "result");
		if (!operationStatus.name().equals(normalizedResult.name())) {
			throw new IllegalArgumentException("Operation status and atomic result must match");
		}
		respondedAt = responseTime;
		completedAt = completionTime;
		String normalizedReference = optionalBoundedText(externalReference, "externalReference", 128);
		if (normalizedReference != null) this.externalReference = normalizedReference;
		this.errorCode = optionalBoundedText(errorCode, "errorCode", 64);
		if (providerCredentialStatus != null && providerCredentialStatus != 0 && providerCredentialStatus != 1) {
			throw new IllegalArgumentException("providerCredentialStatus is invalid");
		}
		this.providerCredentialStatus = providerCredentialStatus;
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
		if (submittedAt != null && submittedAt.isBefore(preparedAt)) throw new IllegalStateException("Submission cannot precede preparation");
		if (respondedAt != null && respondedAt.isBefore(preparedAt)) throw new IllegalStateException("Response cannot precede preparation");
		if (completedAt != null && completedAt.isBefore(preparedAt)) throw new IllegalStateException("Completion cannot precede preparation");
	}

	private static String requireBoundedText(String value, String name, int maxLength) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
		if (value.length() > maxLength) throw new IllegalArgumentException(name + " is too long");
		return value;
	}

	private static String optionalBoundedText(String value, String name, int maxLength) {
		if (value == null) return null;
		String normalized = value.trim();
		if (normalized.isEmpty()) return null;
		return requireBoundedText(normalized, name, maxLength);
	}

	public boolean isSucceeded() { return operationStatus == RevocationOperationStatus.SUCCEEDED; }
	public Long getId() { return id; }
	public DigitalCredentialRevocationRequestEntity getRequest() { return request; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public int getAttemptNumber() { return attemptNumber; }
	public RevocationOperationStatus getOperationStatus() { return operationStatus; }
	public String getExternalReference() { return externalReference; }
	public Instant getPreparedAt() { return preparedAt; }
	public Instant getSubmittedAt() { return submittedAt; }
	public Instant getRespondedAt() { return respondedAt; }
	public Instant getCompletedAt() { return completedAt; }
	public RevocationResult getNormalizedResult() { return normalizedResult; }
	public String getErrorCode() { return errorCode; }
	public Integer getProviderCredentialStatus() { return providerCredentialStatus; }
	public String getCorrelationId() { return correlationId; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
