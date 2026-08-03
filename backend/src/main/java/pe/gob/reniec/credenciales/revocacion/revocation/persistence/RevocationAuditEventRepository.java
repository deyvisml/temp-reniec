package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

import java.util.List;

import org.springframework.data.repository.Repository;

public interface RevocationAuditEventRepository extends Repository<RevocationAuditEventEntity, Long> {

	RevocationAuditEventEntity save(RevocationAuditEventEntity event);

	List<RevocationAuditEventEntity> findByRequest_IdOrderByOccurredAtAscIdAsc(Long requestId);
}
