package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CancellationRequestSessionRepository extends JpaRepository<CancellationRequestSessionEntity, UUID> {

	Optional<CancellationRequestSessionEntity> findBySessionReferenceHash(String sessionReferenceHash);

	List<CancellationRequestSessionEntity> findByRequest_IdAndInvalidatedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
			UUID requestId, Instant cutoff);
}
