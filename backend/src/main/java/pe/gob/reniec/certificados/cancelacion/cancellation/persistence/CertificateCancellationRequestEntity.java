package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "certificate_cancellation_request")
public class CertificateCancellationRequestEntity {

	private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
	private static final Pattern LAST_FOUR = Pattern.compile("[0-9]{4}");

	@Id
	@UuidGenerator
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@Column(name = "dni_lookup_hash", nullable = false, updatable = false, length = 64)
	private String dniLookupHash;

	@Column(name = "dni_ciphertext", nullable = false, length = 512)
	private byte[] dniCiphertext;

	@Column(name = "dni_key_version", nullable = false, length = 64)
	private String dniKeyVersion;

	@Column(name = "dni_last_four", nullable = false, length = 4)
	private String dniLastFour;

	@Enumerated(EnumType.STRING)
	@Column(name = "request_status", nullable = false, length = 48)
	private CancellationRequestStatus requestStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "lifecycle_status", nullable = false, length = 16)
	private RequestLifecycleStatus lifecycleStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "eligibility_result", nullable = false, length = 24)
	private CurrentEligibilityResult eligibilityResult;

	@Enumerated(EnumType.STRING)
	@Column(name = "reason_code", length = 40)
	private CancellationReasonCode reasonCode;

	@Column(name = "other_reason_ciphertext", length = 1024)
	private byte[] otherReasonCiphertext;

	@Column(name = "other_reason_key_version", length = 64)
	private String otherReasonKeyVersion;

	@Column(name = "consent_text_version", length = 64)
	private String consentTextVersion;

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
	private byte[] persistedOtherReason;

	protected CertificateCancellationRequestEntity() {
	}

	public CertificateCancellationRequestEntity(String dniLookupHash, byte[] dniCiphertext,
			String dniKeyVersion, String dniLastFour, Instant recoverableUntil, Instant expiresAt) {
		this.dniLookupHash = requireHash(dniLookupHash, "dniLookupHash");
		this.dniCiphertext = copyRequired(dniCiphertext, "dniCiphertext");
		this.dniKeyVersion = requireText(dniKeyVersion, "dniKeyVersion");
		this.dniLastFour = requirePattern(dniLastFour, LAST_FOUR, "dniLastFour");
		this.recoverableUntil = recoverableUntil;
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
		this.requestStatus = CancellationRequestStatus.STARTED;
		this.lifecycleStatus = RequestLifecycleStatus.ACTIVE;
		this.eligibilityResult = CurrentEligibilityResult.NOT_CHECKED;
	}

	public void recordEligibility(CurrentEligibilityResult result, CancellationRequestStatus status) {
		eligibilityResult = Objects.requireNonNull(result, "result");
		requestStatus = Objects.requireNonNull(status, "status");
	}

	public void registerReason(CancellationReasonCode reason, byte[] protectedDescription, String keyVersion) {
		if (confirmedAt != null) {
			throw new IllegalStateException("A confirmed request cannot change its reason");
		}
		reasonCode = Objects.requireNonNull(reason, "reason");
		if (reason == CancellationReasonCode.OTHER) {
			otherReasonCiphertext = copyRequired(protectedDescription, "protectedDescription");
			otherReasonKeyVersion = requireText(keyVersion, "keyVersion");
		}
		else {
			if (protectedDescription != null || keyVersion != null) {
				throw new IllegalArgumentException("Protected description is only valid for OTHER");
			}
			otherReasonCiphertext = null;
			otherReasonKeyVersion = null;
		}
		requestStatus = CancellationRequestStatus.REASON_REGISTERED;
	}

	public void confirm(String textVersion, Instant confirmationTime) {
		if (reasonCode == null) {
			throw new IllegalStateException("A reason is required before confirmation");
		}
		consentTextVersion = requireText(textVersion, "textVersion");
		confirmedAt = Objects.requireNonNull(confirmationTime, "confirmationTime");
		requestStatus = CancellationRequestStatus.CONFIRMED;
	}

	public void transitionTo(CancellationRequestStatus status, RequestLifecycleStatus lifecycle,
			CancellationFinalOutcome outcome) {
		requestStatus = Objects.requireNonNull(status, "status");
		lifecycleStatus = Objects.requireNonNull(lifecycle, "lifecycle");
		finalOutcome = outcome;
	}

	@PrePersist
	void initializeAndValidate() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
		validateState();
	}

	@PreUpdate
	void updateAndValidate() {
		if (confirmedAt != null && persistedReason != null
				&& (persistedReason != reasonCode || !Arrays.equals(persistedOtherReason, otherReasonCiphertext))) {
			throw new IllegalStateException("A confirmed request cannot change its reason");
		}
		updatedAt = Instant.now();
		validateState();
	}

	@PostLoad
	@PostPersist
	void rememberPersistedReason() {
		persistedReason = reasonCode;
		persistedOtherReason = copy(otherReasonCiphertext);
	}

	private void validateState() {
		requireHash(dniLookupHash, "dniLookupHash");
		copyRequired(dniCiphertext, "dniCiphertext");
		requireText(dniKeyVersion, "dniKeyVersion");
		requirePattern(dniLastFour, LAST_FOUR, "dniLastFour");
		Objects.requireNonNull(requestStatus, "requestStatus");
		Objects.requireNonNull(lifecycleStatus, "lifecycleStatus");
		Objects.requireNonNull(eligibilityResult, "eligibilityResult");
		if (reasonCode == CancellationReasonCode.OTHER) {
			copyRequired(otherReasonCiphertext, "otherReasonCiphertext");
			requireText(otherReasonKeyVersion, "otherReasonKeyVersion");
		}
		else if (otherReasonCiphertext != null || otherReasonKeyVersion != null) {
			throw new IllegalStateException("Only OTHER can retain a protected description");
		}
		if ((consentTextVersion == null) != (confirmedAt == null)) {
			throw new IllegalStateException("Consent version and confirmation time must be stored together");
		}
		if (confirmedAt != null && reasonCode == null) {
			throw new IllegalStateException("A confirmed request requires a reason");
		}
		if (expiresAt == null || !expiresAt.isAfter(createdAt)) {
			throw new IllegalStateException("Request expiry must be after creation");
		}
		if (recoverableUntil != null && recoverableUntil.isBefore(createdAt)) {
			throw new IllegalStateException("Recovery deadline cannot precede creation");
		}
		if (confirmedAt != null && confirmedAt.isBefore(createdAt)) {
			throw new IllegalStateException("Confirmation cannot precede creation");
		}
	}

	private static String requireHash(String value, String name) {
		return requirePattern(value, HASH, name);
	}

	private static String requirePattern(String value, Pattern pattern, String name) {
		String checked = Objects.requireNonNull(value, name);
		if (!pattern.matcher(checked).matches()) {
			throw new IllegalArgumentException(name + " has an invalid format");
		}
		return checked;
	}

	private static String requireText(String value, String name) {
		String checked = Objects.requireNonNull(value, name);
		if (checked.isBlank()) {
			throw new IllegalArgumentException(name + " must not be blank");
		}
		return checked;
	}

	private static byte[] copyRequired(byte[] value, String name) {
		if (value == null || value.length == 0) {
			throw new IllegalArgumentException(name + " must not be empty");
		}
		return value.clone();
	}

	private static byte[] copy(byte[] value) {
		return value == null ? null : value.clone();
	}

	public UUID getId() { return id; }
	public String getDniLookupHash() { return dniLookupHash; }
	public byte[] getDniCiphertext() { return copy(dniCiphertext); }
	public String getDniKeyVersion() { return dniKeyVersion; }
	public String getDniLastFour() { return dniLastFour; }
	public CancellationRequestStatus getRequestStatus() { return requestStatus; }
	public RequestLifecycleStatus getLifecycleStatus() { return lifecycleStatus; }
	public CurrentEligibilityResult getEligibilityResult() { return eligibilityResult; }
	public CancellationReasonCode getReasonCode() { return reasonCode; }
	public byte[] getOtherReasonCiphertext() { return copy(otherReasonCiphertext); }
	public String getOtherReasonKeyVersion() { return otherReasonKeyVersion; }
	public String getConsentTextVersion() { return consentTextVersion; }
	public Instant getConfirmedAt() { return confirmedAt; }
	public CancellationFinalOutcome getFinalOutcome() { return finalOutcome; }
	public Instant getRecoverableUntil() { return recoverableUntil; }
	public Instant getExpiresAt() { return expiresAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public long getVersion() { return version; }
}
