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
@Table(name = "revocation_receipt")
public class RevocationReceiptEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private DigitalCredentialRevocationRequestEntity request;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "revocation_operation_id", nullable = false, updatable = false)
	private RevocationOperationEntity revocationOperation;

	@Column(name = "receipt_code", nullable = false, unique = true, updatable = false, length = 64)
	private String receiptCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "generation_status", nullable = false, length = 16)
	private ReceiptGenerationStatus generationStatus;

	@Column(name = "storage_reference", length = 256)
	private String storageReference;

	@Column(name = "generated_at")
	private Instant generatedAt;

	@Column(name = "available_at")
	private Instant availableAt;

	@Column(name = "error_code", length = 64)
	private String errorCode;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected RevocationReceiptEntity() { }

	public RevocationReceiptEntity(DigitalCredentialRevocationRequestEntity request,
			RevocationOperationEntity operation, String receiptCode) {
		this.request = Objects.requireNonNull(request, "request");
		this.revocationOperation = Objects.requireNonNull(operation, "operation");
		if (!operation.isSucceeded()) throw new IllegalArgumentException("Receipt requires a successful revocation");
		if (!sameRequest(request, operation.getRequest())) throw new IllegalArgumentException("Receipt and revocation must belong to the same request");
		this.receiptCode = requireBoundedText(receiptCode, "receiptCode", 64);
		generationStatus = ReceiptGenerationStatus.PENDING;
	}

	public void markGenerating() { generationStatus = ReceiptGenerationStatus.GENERATING; }

	public void markAvailable(String storageReference, Instant generatedAt, Instant availableAt) {
		this.storageReference = requireBoundedText(storageReference, "storageReference", 256);
		this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
		this.availableAt = Objects.requireNonNull(availableAt, "availableAt");
		errorCode = null;
		generationStatus = ReceiptGenerationStatus.AVAILABLE;
	}

	public void markFailed(String errorCode) {
		this.errorCode = requireBoundedText(errorCode, "errorCode", 64);
		generationStatus = ReceiptGenerationStatus.FAILED;
	}

	@PrePersist
	void initialize() {
		Instant now = Instant.now();
		if (createdAt == null) createdAt = now;
		updatedAt = now;
		validateAvailability();
	}

	@PreUpdate
	void update() { updatedAt = Instant.now(); validateAvailability(); }

	private void validateAvailability() {
		if (availableAt != null && (generatedAt == null || availableAt.isBefore(generatedAt))) {
			throw new IllegalStateException("Availability cannot precede generation");
		}
	}

	private static boolean sameRequest(DigitalCredentialRevocationRequestEntity first,
			DigitalCredentialRevocationRequestEntity second) {
		if (first == second) return true;
		return first.getId() != null && first.getId().equals(second.getId());
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
		return value;
	}

	private static String requireBoundedText(String value, String name, int maxLength) {
		String text = requireText(value, name).trim();
		if (text.length() > maxLength) throw new IllegalArgumentException(name + " is too long");
		return text;
	}

	public Long getId() { return id; }
	public DigitalCredentialRevocationRequestEntity getRequest() { return request; }
	public RevocationOperationEntity getRevocationOperation() { return revocationOperation; }
	public String getReceiptCode() { return receiptCode; }
	public ReceiptGenerationStatus getGenerationStatus() { return generationStatus; }
	public String getStorageReference() { return storageReference; }
	public Instant getGeneratedAt() { return generatedAt; }
	public Instant getAvailableAt() { return availableAt; }
	public String getErrorCode() { return errorCode; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
