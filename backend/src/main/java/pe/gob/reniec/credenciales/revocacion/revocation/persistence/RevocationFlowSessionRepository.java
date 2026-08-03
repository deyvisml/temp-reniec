package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RevocationFlowSessionRepository extends JpaRepository<RevocationFlowSessionEntity, Long> {
	Optional<RevocationFlowSessionEntity> findByRequest_Id(Long requestId);
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select session from RevocationFlowSessionEntity session join fetch session.request where session.id = :id")
	Optional<RevocationFlowSessionEntity> findByIdForUpdate(@Param("id") Long id);
}
