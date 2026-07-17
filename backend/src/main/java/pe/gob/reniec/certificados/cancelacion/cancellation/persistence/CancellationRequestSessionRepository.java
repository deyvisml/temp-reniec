package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CancellationRequestSessionRepository extends JpaRepository<CancellationRequestSessionEntity, Long> {

	Optional<CancellationRequestSessionEntity> findBySessionReference(String sessionReference);

	List<CancellationRequestSessionEntity> findByRequest_IdAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
			Long requestId, Instant cutoff);
}
