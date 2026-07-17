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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "cancellation_receipt")
public class CancellationReceiptEntity {

	private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");

	@Id @UuidGenerator
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private CertificateCancellationRequestEntity request;

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

	@Column(name = "document_hash", length = 64)
	private String documentHash;

	@Column(name = "template_version", nullable = false, updatable = false, length = 64)
	private String templateVersion;

	@Column(name = "generated_at")
	private Instant generatedAt;

	@Column(name = "available_at")
	private Instant availableAt;

	@Column(name = "technical_error_code", length = 64)
	private String technicalErrorCode;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected CancellationReceiptEntity() { }

	public CancellationReceiptEntity(CertificateCancellationRequestEntity request,
			RevocationOperationEntity operation, String receiptCode, String templateVersion) {
		this.request = Objects.requireNonNull(request, "request");
		this.revocationOperation = Objects.requireNonNull(operation, "operation");
		if (!operation.isSucceeded()) throw new IllegalArgumentException("Receipt requires a successful revocation");
		if (!sameRequest(request, operation.getRequest())) throw new IllegalArgumentException("Receipt and revocation must belong to the same request");
		this.receiptCode = requireText(receiptCode, "receiptCode");
		this.templateVersion = requireText(templateVersion, "templateVersion");
		this.generationStatus = ReceiptGenerationStatus.PENDING;
	}

	public void markGenerating() { generationStatus = ReceiptGenerationStatus.GENERATING; }

	public void markAvailable(String storageReference, String documentHash, Instant generatedAt, Instant availableAt) {
		if (documentHash == null || !HASH.matcher(documentHash).matches()) throw new IllegalArgumentException("documentHash has an invalid format");
		this.storageReference = requireText(storageReference, "storageReference");
		this.documentHash = documentHash;
		this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
		this.availableAt = Objects.requireNonNull(availableAt, "availableAt");
		this.technicalErrorCode = null;
		this.generationStatus = ReceiptGenerationStatus.AVAILABLE;
	}

	public void markFailed(String errorCode) {
		technicalErrorCode = requireText(errorCode, "errorCode");
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

	private static boolean sameRequest(CertificateCancellationRequestEntity first,
			CertificateCancellationRequestEntity second) {
		if (first == second) return true;
		return first.getId() != null && first.getId().equals(second.getId());
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
		return value;
	}

	public UUID getId() { return id; }
	public CertificateCancellationRequestEntity getRequest() { return request; }
	public RevocationOperationEntity getRevocationOperation() { return revocationOperation; }
	public String getReceiptCode() { return receiptCode; }
	public ReceiptGenerationStatus getGenerationStatus() { return generationStatus; }
	public String getStorageReference() { return storageReference; }
	public String getDocumentHash() { return documentHash; }
	public String getTemplateVersion() { return templateVersion; }
	public Instant getGeneratedAt() { return generatedAt; }
	public Instant getAvailableAt() { return availableAt; }
	public String getTechnicalErrorCode() { return technicalErrorCode; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
