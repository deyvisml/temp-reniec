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
import jakarta.persistence.Table;

@Entity
@Table(name = "certificate_eligibility_check")
public class CertificateEligibilityCheckEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private CertificateCancellationRequestEntity request;

	@Column(name = "attempt_number", nullable = false, updatable = false)
	private int attemptNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "check_status", nullable = false, length = 16)
	private EligibilityCheckStatus checkStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "normalized_result", length = 24)
	private EligibilityCheckResult normalizedResult;

	@Column(name = "external_reference", length = 128)
	private String externalReference;

	@Column(name = "requested_at", nullable = false, updatable = false)
	private Instant requestedAt;

	@Column(name = "responded_at")
	private Instant respondedAt;

	@Column(name = "error_code", length = 64)
	private String errorCode;

	@Column(name = "correlation_id", nullable = false, updatable = false, length = 64)
	private String correlationId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected CertificateEligibilityCheckEntity() { }

	public CertificateEligibilityCheckEntity(CertificateCancellationRequestEntity request, int attemptNumber,
			EligibilityCheckStatus status, Instant requestedAt, String correlationId) {
		this.request = Objects.requireNonNull(request, "request");
		if (attemptNumber < 1) throw new IllegalArgumentException("attemptNumber must be positive");
		this.attemptNumber = attemptNumber;
		this.checkStatus = Objects.requireNonNull(status, "status");
		this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
		this.correlationId = requireText(correlationId, "correlationId");
	}

	public void complete(EligibilityCheckResult result, Instant responseTime, String externalReference) {
		normalizedResult = Objects.requireNonNull(result, "result");
		respondedAt = Objects.requireNonNull(responseTime, "responseTime");
		this.externalReference = externalReference;
		errorCode = null;
		checkStatus = EligibilityCheckStatus.COMPLETED;
	}

	public void fail(EligibilityCheckResult result, Instant responseTime, String errorCode) {
		normalizedResult = Objects.requireNonNull(result, "result");
		respondedAt = Objects.requireNonNull(responseTime, "responseTime");
		this.errorCode = requireText(errorCode, "errorCode");
		checkStatus = EligibilityCheckStatus.FAILED;
	}

	@PrePersist
	void initialize() {
		if (createdAt == null) createdAt = Instant.now();
		if (respondedAt != null && respondedAt.isBefore(requestedAt)) {
			throw new IllegalStateException("Response cannot precede request");
		}
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
		return value;
	}

	public Long getId() { return id; }
	public CertificateCancellationRequestEntity getRequest() { return request; }
	public int getAttemptNumber() { return attemptNumber; }
	public EligibilityCheckStatus getCheckStatus() { return checkStatus; }
	public EligibilityCheckResult getNormalizedResult() { return normalizedResult; }
	public String getExternalReference() { return externalReference; }
	public Instant getRequestedAt() { return requestedAt; }
	public Instant getRespondedAt() { return respondedAt; }
	public String getErrorCode() { return errorCode; }
	public String getCorrelationId() { return correlationId; }
	public Instant getCreatedAt() { return createdAt; }
}
