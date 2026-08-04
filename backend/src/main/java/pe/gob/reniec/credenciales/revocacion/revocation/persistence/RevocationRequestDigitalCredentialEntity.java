package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

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
@Table(name = "revocation_request_digital_credential")
public class RevocationRequestDigitalCredentialEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private DigitalCredentialRevocationRequestEntity request;

	@Column(name = "legacy_order_number", updatable = false, length = 64)
	private String legacyOrderNumber;

	@Column(name = "status_list_index", updatable = false)
	private Integer statusListIndex;

	@Column(name = "credential_type", updatable = false, length = 100)
	private String credentialType;

	@Column(name = "provider_credential_status")
	private Integer providerCredentialStatus;

	@Column(name = "emission_created_at", nullable = false, updatable = false)
	private Instant emissionCreatedAt;

	@Column(name = "digital_credential_uuid", nullable = false, updatable = false, length = 36)
	private String digitalCredentialUuid;

	@Enumerated(EnumType.STRING)
	@Column(name = "availability_status", nullable = false, length = 32)
	private DigitalCredentialAvailabilityStatus availabilityStatus;

	@Column(name = "consulted_at", nullable = false, updatable = false)
	private Instant consultedAt;

	@Column(name = "selected", nullable = false)
	private boolean selected;

	@Column(name = "selected_at")
	private Instant selectedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

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

	protected RevocationRequestDigitalCredentialEntity() { }

	public RevocationRequestDigitalCredentialEntity(DigitalCredentialRevocationRequestEntity request,
			String orderNumber, Instant emissionCreatedAt,
			String digitalCredentialUuid, Instant consultedAt) {
		this(request, orderNumber, emissionCreatedAt, digitalCredentialUuid,
				DigitalCredentialAvailabilityStatus.AVAILABLE, null, consultedAt);
	}

	public RevocationRequestDigitalCredentialEntity(DigitalCredentialRevocationRequestEntity request,
			String orderNumber, Instant emissionCreatedAt, String digitalCredentialUuid,
			DigitalCredentialAvailabilityStatus availabilityStatus, Instant revokedAt, Instant consultedAt) {
		this.request = Objects.requireNonNull(request, "request");
		this.legacyOrderNumber = requireAsciiText(orderNumber, "orderNumber", 64);
		this.emissionCreatedAt = Objects.requireNonNull(emissionCreatedAt, "emissionCreatedAt");
		this.digitalCredentialUuid = canonicalUuid(digitalCredentialUuid);
		this.consultedAt = Objects.requireNonNull(consultedAt, "consultedAt");
		this.availabilityStatus = Objects.requireNonNull(availabilityStatus, "availabilityStatus");
		this.revokedAt = revokedAt;
		validateTimes();
	}

	public RevocationRequestDigitalCredentialEntity(DigitalCredentialRevocationRequestEntity request,
			int statusListIndex, String credentialType, Instant emissionCreatedAt,
			String digitalCredentialUuid, DigitalCredentialAvailabilityStatus availabilityStatus,
			Instant revokedAt, int providerCredentialStatus, Instant consultedAt) {
		this.request = Objects.requireNonNull(request, "request");
		if (statusListIndex < 0) throw new IllegalArgumentException("statusListIndex must not be negative");
		this.statusListIndex = statusListIndex;
		this.credentialType = requireText(credentialType, "credentialType", 100);
		this.providerCredentialStatus = requireProviderStatus(providerCredentialStatus);
		this.emissionCreatedAt = Objects.requireNonNull(emissionCreatedAt, "emissionCreatedAt");
		this.digitalCredentialUuid = canonicalUuid(digitalCredentialUuid);
		this.consultedAt = Objects.requireNonNull(consultedAt, "consultedAt");
		this.availabilityStatus = Objects.requireNonNull(availabilityStatus, "availabilityStatus");
		this.revokedAt = revokedAt;
		validateTimes();
	}

	public void select(Instant at) {
		ensureSelectionMutable();
		if (availabilityStatus != DigitalCredentialAvailabilityStatus.AVAILABLE) {
			throw new IllegalStateException("Only an available digitalCredential can be selected");
		}
		selected = true;
		selectedAt = Objects.requireNonNull(at, "at");
	}

	public void deselect() {
		ensureSelectionMutable();
		if (availabilityStatus != DigitalCredentialAvailabilityStatus.AVAILABLE) {
			throw new IllegalStateException("Only an available digitalCredential can be deselected");
		}
		selected = false;
		selectedAt = null;
	}

	public void changeAvailability(DigitalCredentialAvailabilityStatus status, Instant revokedAt) {
		availabilityStatus = Objects.requireNonNull(status, "status");
		this.revokedAt = revokedAt;
	}

	public void applyAtomicOutcome(RevocationResult result, Instant completedAt, Integer providerStatus) {
		if (!selected) {
			throw new IllegalStateException("Only selected digitalCredentials belong to the atomic operation");
		}
		availabilityStatus = switch (Objects.requireNonNull(result, "result")) {
			case SUCCEEDED -> DigitalCredentialAvailabilityStatus.REVOKED;
			case FAILED -> DigitalCredentialAvailabilityStatus.REVOCATION_FAILED;
			case OUTCOME_UNKNOWN -> DigitalCredentialAvailabilityStatus.OUTCOME_UNKNOWN;
		};
		revokedAt = result == RevocationResult.SUCCEEDED
				? Objects.requireNonNull(completedAt, "completedAt") : null;
		providerCredentialStatus = providerStatus == null ? null : requireProviderStatus(providerStatus);
	}

	public void applyAtomicOutcome(RevocationResult result, Instant completedAt) {
		applyAtomicOutcome(result, completedAt, null);
	}

	@PrePersist
	void initialize() {
		if (request.getConfirmedAt() != null) {
			throw new IllegalStateException("DigitalCredentials cannot be added after confirmation");
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
			throw new IllegalStateException("A confirmed digitalCredential selection cannot be changed");
		}
		updatedAt = Instant.now();
		validate();
	}

	@PreRemove
	void preventRemovalAfterConfirmation() {
		if (request.getConfirmedAt() != null) {
			throw new IllegalStateException("A confirmed digitalCredential selection cannot be removed");
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
			throw new IllegalStateException("A confirmed digitalCredential selection cannot be changed");
		}
	}

	private void validate() {
		if (legacyOrderNumber != null) legacyOrderNumber = requireAsciiText(legacyOrderNumber, "legacyOrderNumber", 64);
		if (statusListIndex != null && statusListIndex < 0) throw new IllegalStateException("statusListIndex must not be negative");
		if (credentialType != null) credentialType = requireText(credentialType, "credentialType", 100);
		if (providerCredentialStatus != null) providerCredentialStatus = requireProviderStatus(providerCredentialStatus);
		digitalCredentialUuid = canonicalUuid(digitalCredentialUuid);
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
		if (availabilityStatus == DigitalCredentialAvailabilityStatus.REVOKED) {
			if (revokedAt != null && revokedAt.isBefore(emissionCreatedAt)) {
				throw new IllegalArgumentException("revokedAt cannot precede emissionCreatedAt");
			}
		}
		else if (revokedAt != null) {
			throw new IllegalArgumentException("Only a revoked digitalCredential can have revokedAt");
		}
	}

	private static String canonicalUuid(String value) {
		if (value == null) throw new IllegalArgumentException("digitalCredentialUuid must not be null");
		String normalized = value.toLowerCase(Locale.ROOT);
		try {
			UUID parsed = UUID.fromString(normalized);
			if (!parsed.toString().equals(normalized)) throw new IllegalArgumentException();
			return normalized;
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("digitalCredentialUuid must use canonical UUID format", exception);
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

	private static String requireText(String value, String name, int maxLength) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
		String text = value.trim();
		if (text.length() > maxLength) throw new IllegalArgumentException(name + " is too long");
		return text;
	}

	private static int requireProviderStatus(int value) {
		if (value != 0 && value != 1) throw new IllegalArgumentException("providerCredentialStatus is invalid");
		return value;
	}

	public Long getId() { return id; }
	public DigitalCredentialRevocationRequestEntity getRequest() { return request; }
	public String getLegacyOrderNumber() { return legacyOrderNumber; }
	public Integer getStatusListIndex() { return statusListIndex; }
	public String getCredentialType() { return credentialType; }
	public Integer getProviderCredentialStatus() { return providerCredentialStatus; }
	public Instant getEmissionCreatedAt() { return emissionCreatedAt; }
	public String getDigitalCredentialUuid() { return digitalCredentialUuid; }
	public DigitalCredentialAvailabilityStatus getAvailabilityStatus() { return availabilityStatus; }
	public Instant getConsultedAt() { return consultedAt; }
	public boolean isSelected() { return selected; }
	public Instant getSelectedAt() { return selectedAt; }
	public Instant getRevokedAt() { return revokedAt; }
	public long getVersion() { return version; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
