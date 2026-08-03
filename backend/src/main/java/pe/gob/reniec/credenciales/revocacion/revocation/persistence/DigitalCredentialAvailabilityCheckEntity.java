package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

import java.nio.charset.StandardCharsets;
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
@Table(name = "digital_credential_availability_check")
public class DigitalCredentialAvailabilityCheckEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private DigitalCredentialRevocationRequestEntity request;

	@Column(name = "attempt_number", nullable = false, updatable = false)
	private int attemptNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "check_status", nullable = false, length = 16)
	private AvailabilityCheckStatus checkStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "normalized_result", length = 24)
	private AvailabilityCheckResult normalizedResult;

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

	protected DigitalCredentialAvailabilityCheckEntity() { }

	public DigitalCredentialAvailabilityCheckEntity(DigitalCredentialRevocationRequestEntity request, int attemptNumber,
			AvailabilityCheckStatus status, Instant requestedAt, String correlationId) {
		this.request = Objects.requireNonNull(request, "request");
		if (attemptNumber < 1) throw new IllegalArgumentException("attemptNumber must be positive");
		this.attemptNumber = attemptNumber;
		this.checkStatus = Objects.requireNonNull(status, "status");
		this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
		this.correlationId = requireText(correlationId, "correlationId");
	}

	public void complete(AvailabilityCheckResult result, Instant responseTime, String externalReference) {
		ensureSubmitted();
		Objects.requireNonNull(result, "result");
		if (result == AvailabilityCheckResult.UNAVAILABLE || result == AvailabilityCheckResult.ERROR) {
			throw new IllegalArgumentException("A failed result cannot complete an availability check");
		}
		normalizedResult = result;
		respondedAt = Objects.requireNonNull(responseTime, "responseTime");
		this.externalReference = optionalAsciiText(externalReference, "externalReference", 128);
		errorCode = null;
		checkStatus = AvailabilityCheckStatus.COMPLETED;
	}

	public void fail(AvailabilityCheckResult result, Instant responseTime, String errorCode) {
		ensureSubmitted();
		Objects.requireNonNull(result, "result");
		if (result != AvailabilityCheckResult.UNAVAILABLE && result != AvailabilityCheckResult.ERROR) {
			throw new IllegalArgumentException("A successful result cannot fail an availability check");
		}
		normalizedResult = result;
		respondedAt = Objects.requireNonNull(responseTime, "responseTime");
		this.errorCode = requireAsciiText(errorCode, "errorCode", 64);
		externalReference = null;
		checkStatus = AvailabilityCheckStatus.FAILED;
	}

	private void ensureSubmitted() {
		if (checkStatus != AvailabilityCheckStatus.SUBMITTED) {
			throw new IllegalStateException("Only a submitted availability check can be finalized");
		}
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
		return value.trim();
	}

	private static String requireAsciiText(String value, String name, int maxLength) {
		String text = requireText(value, name);
		if (text.length() > maxLength) throw new IllegalArgumentException(name + " is too long");
		if (!StandardCharsets.US_ASCII.newEncoder().canEncode(text)) {
			throw new IllegalArgumentException(name + " must contain ASCII characters");
		}
		return text;
	}

	private static String optionalAsciiText(String value, String name, int maxLength) {
		if (value == null || value.isBlank()) return null;
		return requireAsciiText(value, name, maxLength);
	}

	public Long getId() { return id; }
	public DigitalCredentialRevocationRequestEntity getRequest() { return request; }
	public int getAttemptNumber() { return attemptNumber; }
	public AvailabilityCheckStatus getCheckStatus() { return checkStatus; }
	public AvailabilityCheckResult getNormalizedResult() { return normalizedResult; }
	public String getExternalReference() { return externalReference; }
	public Instant getRequestedAt() { return requestedAt; }
	public Instant getRespondedAt() { return respondedAt; }
	public String getErrorCode() { return errorCode; }
	public String getCorrelationId() { return correlationId; }
	public Instant getCreatedAt() { return createdAt; }
}
