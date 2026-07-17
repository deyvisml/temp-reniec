package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.Repository;

public interface CancellationAuditEventRepository extends Repository<CancellationAuditEventEntity, UUID> {

	CancellationAuditEventEntity save(CancellationAuditEventEntity event);

	List<CancellationAuditEventEntity> findByRequest_IdOrderByOccurredAtAscIdAsc(UUID requestId);
}
