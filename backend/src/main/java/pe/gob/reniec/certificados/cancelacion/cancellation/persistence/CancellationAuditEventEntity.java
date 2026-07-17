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
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "cancellation_audit_event")
public class CancellationAuditEventEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false, updatable = false)
	private CertificateCancellationRequestEntity request;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, updatable = false, length = 64)
	private CancellationAuditEventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(name = "previous_status", updatable = false, length = 48)
	private CancellationRequestStatus previousStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "new_status", updatable = false, length = 48)
	private CancellationRequestStatus newStatus;

	@Column(name = "result", updatable = false, length = 64)
	private String result;

	@Column(name = "correlation_id", nullable = false, updatable = false, length = 64)
	private String correlationId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_origin", nullable = false, updatable = false, length = 32)
	private AuditEventOrigin eventOrigin;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	protected CancellationAuditEventEntity() { }

	public CancellationAuditEventEntity(CertificateCancellationRequestEntity request,
			CancellationAuditEventType eventType, CancellationRequestStatus previousStatus,
			CancellationRequestStatus newStatus, String result, String correlationId,
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
	public CertificateCancellationRequestEntity getRequest() { return request; }
	public CancellationAuditEventType getEventType() { return eventType; }
	public CancellationRequestStatus getPreviousStatus() { return previousStatus; }
	public CancellationRequestStatus getNewStatus() { return newStatus; }
	public String getResult() { return result; }
	public String getCorrelationId() { return correlationId; }
	public AuditEventOrigin getEventOrigin() { return eventOrigin; }
	public Instant getOccurredAt() { return occurredAt; }
}
