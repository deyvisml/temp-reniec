package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

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
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

@Entity
@Table(name = "cancellation_request_certificate")
public class CancellationRequestCertificateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private CertificateCancellationRequestEntity request;

	@Column(name = "order_number", nullable = false, updatable = false, length = 64)
	private String orderNumber;

	@Column(name = "emission_created_at", nullable = false, updatable = false)
	private Instant emissionCreatedAt;

	@Column(name = "certificate_uuid", nullable = false, updatable = false, length = 36)
	private String certificateUuid;

	@Enumerated(EnumType.STRING)
	@Column(name = "availability_status", nullable = false, length = 32)
	private CertificateAvailabilityStatus availabilityStatus;

	@Column(name = "consulted_at", nullable = false, updatable = false)
	private Instant consultedAt;

	@Column(name = "selected", nullable = false)
	private boolean selected;

	@Column(name = "selected_at")
	private Instant selectedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Transient
	private Boolean persistedSelected;

	@Transient
	private Instant persistedSelectedAt;

	protected CancellationRequestCertificateEntity() { }

	public CancellationRequestCertificateEntity(CertificateCancellationRequestEntity request,
			String orderNumber, Instant emissionCreatedAt,
			String certificateUuid, Instant consultedAt) {
		this.request = Objects.requireNonNull(request, "request");
		this.orderNumber = requireAsciiText(orderNumber, "orderNumber", 64);
		this.emissionCreatedAt = Objects.requireNonNull(emissionCreatedAt, "emissionCreatedAt");
		this.certificateUuid = canonicalUuid(certificateUuid);
		this.consultedAt = Objects.requireNonNull(consultedAt, "consultedAt");
		this.availabilityStatus = CertificateAvailabilityStatus.AVAILABLE;
		validateTimes();
	}

	public void select(Instant at) {
		ensureSelectionMutable();
		if (availabilityStatus != CertificateAvailabilityStatus.AVAILABLE) {
			throw new IllegalStateException("Only an available certificate can be selected");
		}
		selected = true;
		selectedAt = Objects.requireNonNull(at, "at");
	}

	public void deselect() {
		ensureSelectionMutable();
		if (availabilityStatus != CertificateAvailabilityStatus.AVAILABLE) {
			throw new IllegalStateException("Only an available certificate can be deselected");
		}
		selected = false;
		selectedAt = null;
	}

	public void changeAvailability(CertificateAvailabilityStatus status) {
		availabilityStatus = Objects.requireNonNull(status, "status");
	}

	public void applyAtomicOutcome(RevocationResult result) {
		if (!selected) {
			throw new IllegalStateException("Only selected certificates belong to the atomic operation");
		}
		availabilityStatus = switch (Objects.requireNonNull(result, "result")) {
			case SUCCEEDED -> CertificateAvailabilityStatus.REVOKED;
			case FAILED -> CertificateAvailabilityStatus.REVOCATION_FAILED;
			case OUTCOME_UNKNOWN -> CertificateAvailabilityStatus.OUTCOME_UNKNOWN;
		};
	}

	@PrePersist
	void initialize() {
		if (request.getConfirmedAt() != null) {
			throw new IllegalStateException("Certificates cannot be added after confirmation");
		}
		Instant now = Instant.now();
		if (createdAt == null) createdAt = now;
		updatedAt = now;
		validate();
	}

	@PreUpdate
	void update() {
		if (request.wasConfirmedBeforeCurrentUnitOfWork() && persistedSelected != null
				&& (persistedSelected != selected || !Objects.equals(persistedSelectedAt, selectedAt))) {
			throw new IllegalStateException("A confirmed certificate selection cannot be changed");
		}
		updatedAt = Instant.now();
		validate();
	}

	@PreRemove
	void preventRemovalAfterConfirmation() {
		if (request.getConfirmedAt() != null) {
			throw new IllegalStateException("A confirmed certificate selection cannot be removed");
		}
	}

	@PostPersist
	@PostLoad
	@PostUpdate
	void rememberSelection() {
		persistedSelected = selected;
		persistedSelectedAt = selectedAt;
	}

	private void ensureSelectionMutable() {
		if (request.getConfirmedAt() != null) {
			throw new IllegalStateException("A confirmed certificate selection cannot be changed");
		}
	}

	private void validate() {
		orderNumber = requireAsciiText(orderNumber, "orderNumber", 64);
		certificateUuid = canonicalUuid(certificateUuid);
		Objects.requireNonNull(availabilityStatus, "availabilityStatus");
		if (selected != (selectedAt != null)) {
			throw new IllegalStateException("selected and selectedAt must be consistent");
		}
		validateTimes();
	}

	private void validateTimes() {
		if (consultedAt.isBefore(emissionCreatedAt)) {
			throw new IllegalArgumentException("consultedAt cannot precede emissionCreatedAt");
		}
	}

	private static String canonicalUuid(String value) {
		if (value == null) throw new IllegalArgumentException("certificateUuid must not be null");
		String normalized = value.toLowerCase(Locale.ROOT);
		try {
			UUID parsed = UUID.fromString(normalized);
			if (!parsed.toString().equals(normalized)) throw new IllegalArgumentException();
			return normalized;
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("certificateUuid must use canonical UUID format", exception);
		}
	}

	private static String requireAsciiText(String value, String name, int maxLength) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
		String text = value.trim();
		if (text.length() > maxLength) throw new IllegalArgumentException(name + " is too long");
		if (!StandardCharsets.US_ASCII.newEncoder().canEncode(text)) {
			throw new IllegalArgumentException(name + " must contain ASCII characters");
		}
		return text;
	}

	public Long getId() { return id; }
	public CertificateCancellationRequestEntity getRequest() { return request; }
	public String getOrderNumber() { return orderNumber; }
	public Instant getEmissionCreatedAt() { return emissionCreatedAt; }
	public String getCertificateUuid() { return certificateUuid; }
	public CertificateAvailabilityStatus getAvailabilityStatus() { return availabilityStatus; }
	public Instant getConsultedAt() { return consultedAt; }
	public boolean isSelected() { return selected; }
	public Instant getSelectedAt() { return selectedAt; }
	public long getVersion() { return version; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
