package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CancellationFlowSessionRepository extends JpaRepository<CancellationFlowSessionEntity, Long> {
	Optional<CancellationFlowSessionEntity> findByRequest_Id(Long requestId);
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select session from CancellationFlowSessionEntity session join fetch session.request where session.id = :id")
	Optional<CancellationFlowSessionEntity> findByIdForUpdate(@Param("id") Long id);
}
