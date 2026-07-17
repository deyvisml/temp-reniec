package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.List;

import org.springframework.data.repository.Repository;

public interface CancellationAuditEventRepository extends Repository<CancellationAuditEventEntity, Long> {

	CancellationAuditEventEntity save(CancellationAuditEventEntity event);

	List<CancellationAuditEventEntity> findByRequest_IdOrderByOccurredAtAscIdAsc(Long requestId);
}
