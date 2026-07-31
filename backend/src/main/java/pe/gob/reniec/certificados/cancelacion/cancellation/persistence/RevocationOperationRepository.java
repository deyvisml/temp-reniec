package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RevocationOperationRepository extends JpaRepository<RevocationOperationEntity, Long> {

	Optional<RevocationOperationEntity> findByIdempotencyKey(String idempotencyKey);

	Optional<RevocationOperationEntity> findFirstByRequest_IdOrderByAttemptNumberDesc(Long requestId);

	Optional<RevocationOperationEntity> findFirstByRequest_IdAndOperationStatusInOrderByAttemptNumberDesc(
			Long requestId, Collection<RevocationOperationStatus> statuses);
}
