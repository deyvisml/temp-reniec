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
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "revocation_audit_event")
public class RevocationAuditEventEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private DigitalCredentialRevocationRequestEntity request;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, updatable = false, length = 64)
	private RevocationAuditEventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(name = "previous_status", updatable = false, length = 48)
	private RevocationRequestStatus previousStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "new_status", updatable = false, length = 48)
	private RevocationRequestStatus newStatus;

	@Column(name = "result", updatable = false, length = 64)
	private String result;

	@Column(name = "correlation_id", nullable = false, updatable = false, length = 64)
	private String correlationId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_origin", nullable = false, updatable = false, length = 32)
	private AuditEventOrigin eventOrigin;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	protected RevocationAuditEventEntity() { }

	public RevocationAuditEventEntity(DigitalCredentialRevocationRequestEntity request,
			RevocationAuditEventType eventType, RevocationRequestStatus previousStatus,
			RevocationRequestStatus newStatus, String result, String correlationId,
			AuditEventOrigin eventOrigin, Instant occurredAt) {
		this.request = Objects.requireNonNull(request, "request");
		this.eventType = Objects.requireNonNull(eventType, "eventType");
		this.previousStatus = previousStatus;
		this.newStatus = newStatus;
		this.result = result;
		if (correlationId == null || correlationId.isBlank()) throw new IllegalArgumentException("correlationId must not be blank");
		this.correlationId = correlationId;
		this.eventOrigin = Objects.requireNonNull(eventOrigin, "eventOrigin");
		this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
	}

	public Long getId() { return id; }
	public DigitalCredentialRevocationRequestEntity getRequest() { return request; }
	public RevocationAuditEventType getEventType() { return eventType; }
	public RevocationRequestStatus getPreviousStatus() { return previousStatus; }
	public RevocationRequestStatus getNewStatus() { return newStatus; }
	public String getResult() { return result; }
	public String getCorrelationId() { return correlationId; }
	public AuditEventOrigin getEventOrigin() { return eventOrigin; }
	public Instant getOccurredAt() { return occurredAt; }
}
