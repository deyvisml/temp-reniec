package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "revocation_operation")
public class RevocationOperationEntity {

	@Id @UuidGenerator
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private CertificateCancellationRequestEntity request;

	@Column(name = "idempotency_key", nullable = false, unique = true, updatable = false, columnDefinition = "BINARY(16)")
	private UUID idempotencyKey;

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

	@Column(name = "technical_error_code", length = 64)
	private String technicalErrorCode;

	@Column(name = "next_status_check_at")
	private Instant nextStatusCheckAt;

	@Column(name = "correlation_id", nullable = false, updatable = false, length = 64)
	private String correlationId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected RevocationOperationEntity() { }

	public RevocationOperationEntity(CertificateCancellationRequestEntity request, UUID idempotencyKey,
			int attemptNumber, Instant preparedAt, String correlationId) {
		this.request = Objects.requireNonNull(request, "request");
		this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
		if (attemptNumber < 1) throw new IllegalArgumentException("attemptNumber must be positive");
		this.attemptNumber = attemptNumber;
		this.preparedAt = Objects.requireNonNull(preparedAt, "preparedAt");
		if (correlationId == null || correlationId.isBlank()) throw new IllegalArgumentException("correlationId must not be blank");
		this.correlationId = correlationId;
		this.operationStatus = RevocationOperationStatus.PREPARED;
	}

	public void markSubmitted(Instant at, String externalReference) {
		operationStatus = RevocationOperationStatus.SUBMITTED;
		submittedAt = Objects.requireNonNull(at, "at");
		this.externalReference = externalReference;
	}

	public void complete(RevocationOperationStatus status, RevocationResult result, Instant responseTime,
			Instant completionTime, String errorCode, Instant nextStatusCheckAt) {
		if (status == RevocationOperationStatus.PREPARED || status == RevocationOperationStatus.SUBMITTED) {
			throw new IllegalArgumentException("Completion requires a terminal or uncertain status");
		}
		operationStatus = Objects.requireNonNull(status, "status");
		normalizedResult = Objects.requireNonNull(result, "result");
		respondedAt = responseTime;
		completedAt = completionTime;
		technicalErrorCode = errorCode;
		this.nextStatusCheckAt = nextStatusCheckAt;
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

	public boolean isSucceeded() { return operationStatus == RevocationOperationStatus.SUCCEEDED; }
	public UUID getId() { return id; }
	public CertificateCancellationRequestEntity getRequest() { return request; }
	public UUID getIdempotencyKey() { return idempotencyKey; }
	public int getAttemptNumber() { return attemptNumber; }
	public RevocationOperationStatus getOperationStatus() { return operationStatus; }
	public String getExternalReference() { return externalReference; }
	public Instant getPreparedAt() { return preparedAt; }
	public Instant getSubmittedAt() { return submittedAt; }
	public Instant getRespondedAt() { return respondedAt; }
	public Instant getCompletedAt() { return completedAt; }
	public RevocationResult getNormalizedResult() { return normalizedResult; }
	public String getTechnicalErrorCode() { return technicalErrorCode; }
	public Instant getNextStatusCheckAt() { return nextStatusCheckAt; }
	public String getCorrelationId() { return correlationId; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public long getVersion() { return version; }
}
