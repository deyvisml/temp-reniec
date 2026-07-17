package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

@Entity
@Table(name = "certificate_cancellation_request")
public class CertificateCancellationRequestEntity {

	private static final Pattern DNI = Pattern.compile("[0-9]{8}");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@Column(name = "dni", nullable = false, updatable = false, length = 8)
	private String dni;

	@Enumerated(EnumType.STRING)
	@Column(name = "request_status", nullable = false, length = 48)
	private CancellationRequestStatus requestStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "eligibility_result", nullable = false, length = 24)
	private CurrentEligibilityResult eligibilityResult;

	@Enumerated(EnumType.STRING)
	@Column(name = "reason_code", length = 40)
	private CancellationReasonCode reasonCode;

	@Column(name = "other_reason", length = 300)
	private String otherReason;

	@Column(name = "consent_version", length = 64)
	private String consentVersion;

	@Column(name = "confirmed_at")
	private Instant confirmedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "final_outcome", length = 40)
	private CancellationFinalOutcome finalOutcome;

	@Column(name = "recoverable_until")
	private Instant recoverableUntil;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private CancellationReasonCode persistedReason;

	@Transient
	private String persistedOtherReason;

	@Transient
	private Instant persistedConfirmedAt;

	protected CertificateCancellationRequestEntity() {
	}

	public CertificateCancellationRequestEntity(String dni, Instant recoverableUntil, Instant expiresAt) {
		this.dni = requireDni(dni);
		this.recoverableUntil = recoverableUntil;
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
		this.requestStatus = CancellationRequestStatus.STARTED;
		this.eligibilityResult = CurrentEligibilityResult.NOT_CHECKED;
	}

	public void recordEligibility(CurrentEligibilityResult result, CancellationRequestStatus status) {
		eligibilityResult = Objects.requireNonNull(result, "result");
		requestStatus = Objects.requireNonNull(status, "status");
	}

	public void registerReason(CancellationReasonCode reason, String description) {
		ensureReasonMutable();
		reasonCode = Objects.requireNonNull(reason, "reason");
		if (reason == CancellationReasonCode.OTHER) {
			otherReason = requireBoundedText(description, "otherReason", 300);
		}
		else {
			if (description != null) throw new IllegalArgumentException("otherReason is only valid for OTHER");
			otherReason = null;
		}
		requestStatus = CancellationRequestStatus.REASON_REGISTERED;
	}

	public void confirm(String consentVersion, Instant confirmedAt) {
		if (reasonCode == null) throw new IllegalStateException("A reason is required before confirmation");
		this.consentVersion = requireText(consentVersion, "consentVersion");
		this.confirmedAt = Objects.requireNonNull(confirmedAt, "confirmedAt");
		requestStatus = CancellationRequestStatus.CONFIRMED;
	}

	public void transitionTo(CancellationRequestStatus status, CancellationFinalOutcome outcome) {
		requestStatus = Objects.requireNonNull(status, "status");
		finalOutcome = outcome;
	}

	@PrePersist
	void initialize() {
		Instant now = Instant.now();
		if (createdAt == null) createdAt = now;
		updatedAt = now;
		validate();
	}

	@PostPersist
	@PostLoad
	@PostUpdate
	void rememberReason() {
		persistedReason = reasonCode;
		persistedOtherReason = otherReason;
		persistedConfirmedAt = confirmedAt;
	}

	@PreUpdate
	void update() {
		if (persistedConfirmedAt != null
				&& (persistedReason != reasonCode || !Objects.equals(persistedOtherReason, otherReason))) {
			throw new IllegalStateException("A confirmed reason cannot be changed");
		}
		updatedAt = Instant.now();
		validate();
	}

	private void validate() {
		requireDni(dni);
		if (!expiresAt.isAfter(createdAt)) throw new IllegalStateException("Expiry must be after creation");
		if (recoverableUntil != null && recoverableUntil.isBefore(createdAt)) {
			throw new IllegalStateException("Recovery cannot end before creation");
		}
		if (confirmedAt != null) {
			if (reasonCode == null || consentVersion == null) {
				throw new IllegalStateException("Confirmation requires reason and consent version");
			}
			if (confirmedAt.isBefore(createdAt)) throw new IllegalStateException("Confirmation cannot precede creation");
		}
		if (reasonCode == CancellationReasonCode.OTHER) requireBoundedText(otherReason, "otherReason", 300);
		else if (otherReason != null) throw new IllegalStateException("otherReason is only valid for OTHER");
	}

	private void ensureReasonMutable() {
		if (confirmedAt != null) throw new IllegalStateException("A confirmed reason cannot be changed");
	}

	private static String requireDni(String value) {
		if (value == null || !DNI.matcher(value).matches()) {
			throw new IllegalArgumentException("dni must contain exactly eight digits");
		}
		return value;
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
		return value;
	}

	private static String requireBoundedText(String value, String name, int maxLength) {
		String text = requireText(value, name);
		if (text.length() > maxLength) throw new IllegalArgumentException(name + " is too long");
		return text;
	}

	public Long getId() { return id; }
	public String getDni() { return dni; }
	public CancellationRequestStatus getRequestStatus() { return requestStatus; }
	public CurrentEligibilityResult getEligibilityResult() { return eligibilityResult; }
	public CancellationReasonCode getReasonCode() { return reasonCode; }
	public String getOtherReason() { return otherReason; }
	public String getConsentVersion() { return consentVersion; }
	public Instant getConfirmedAt() { return confirmedAt; }
	public CancellationFinalOutcome getFinalOutcome() { return finalOutcome; }
	public Instant getRecoverableUntil() { return recoverableUntil; }
	public Instant getExpiresAt() { return expiresAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public long getVersion() { return version; }
}
