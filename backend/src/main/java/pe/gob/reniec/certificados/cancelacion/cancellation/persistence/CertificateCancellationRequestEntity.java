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
	@Column(name = "availability_result", nullable = false, length = 24)
	private CurrentAvailabilityResult availabilityResult;

	@Enumerated(EnumType.STRING)
	@Column(name = "reason_code", length = 40)
	private CancellationReasonCode reasonCode;

	@Column(name = "other_reason", length = 300)
	private String otherReason;

	@Column(name = "confirmed_at")
	private Instant confirmedAt;

	@Column(name = "consent_version", length = 64)
	private String consentVersion;

	@Enumerated(EnumType.STRING)
	@Column(name = "final_outcome", length = 40)
	private CancellationFinalOutcome finalOutcome;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Transient
	private CancellationReasonCode persistedReason;

	@Transient
	private String persistedOtherReason;

	@Transient
	private Instant persistedConfirmedAt;

	@Transient
	private String persistedConsentVersion;

	protected CertificateCancellationRequestEntity() {
	}

	public CertificateCancellationRequestEntity(String dni) {
		this.dni = requireDni(dni);
		this.requestStatus = CancellationRequestStatus.STARTED;
		this.availabilityResult = CurrentAvailabilityResult.NOT_CHECKED;
	}

	public void recordAvailability(CurrentAvailabilityResult result, CancellationRequestStatus status) {
		availabilityResult = Objects.requireNonNull(result, "result");
		requestStatus = Objects.requireNonNull(status, "status");
	}

	public void beginAvailabilityCheck() {
		requestStatus = CancellationRequestStatus.CHECKING_AVAILABILITY;
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

	public void clearReasonForCertificateReselection() {
		ensureReasonMutable();
		reasonCode = null;
		otherReason = null;
		requestStatus = CancellationRequestStatus.CERTIFICATES_SELECTED;
	}

	public void confirm(Instant confirmedAt, String consentVersion) {
		if (reasonCode == null) throw new IllegalStateException("A reason is required before confirmation");
		if (this.confirmedAt != null) throw new IllegalStateException("The request is already confirmed");
		this.confirmedAt = Objects.requireNonNull(confirmedAt, "confirmedAt");
		this.consentVersion = requireBoundedText(consentVersion, "consentVersion", 64);
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
		persistedConsentVersion = consentVersion;
	}

	@PreUpdate
	void update() {
		if (persistedConfirmedAt != null && (persistedReason != reasonCode
				|| !Objects.equals(persistedOtherReason, otherReason)
				|| !Objects.equals(persistedConfirmedAt, confirmedAt)
				|| !Objects.equals(persistedConsentVersion, consentVersion))) {
			throw new IllegalStateException("Confirmed request data cannot be changed");
		}
		updatedAt = Instant.now();
		validate();
	}

	private void validate() {
		requireDni(dni);
		if (confirmedAt != null) {
			if (reasonCode == null) {
				throw new IllegalStateException("Confirmation requires reason");
			}
			if (consentVersion != null) requireBoundedText(consentVersion, "consentVersion", 64);
			if (confirmedAt.isBefore(createdAt)) throw new IllegalStateException("Confirmation cannot precede creation");
		}
		else if (consentVersion != null) throw new IllegalStateException("Consent version requires confirmation");
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
	public CurrentAvailabilityResult getAvailabilityResult() { return availabilityResult; }
	public CancellationReasonCode getReasonCode() { return reasonCode; }
	public String getOtherReason() { return otherReason; }
	public Instant getConfirmedAt() { return confirmedAt; }
	public String getConsentVersion() { return consentVersion; }
	public CancellationFinalOutcome getFinalOutcome() { return finalOutcome; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
