package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RevocationOperationRepository extends JpaRepository<RevocationOperationEntity, UUID> {

	Optional<RevocationOperationEntity> findByIdempotencyKey(UUID idempotencyKey);

	Optional<RevocationOperationEntity> findFirstByRequest_IdAndOperationStatusInOrderByAttemptNumberDesc(
			UUID requestId, Collection<RevocationOperationStatus> statuses);
}
