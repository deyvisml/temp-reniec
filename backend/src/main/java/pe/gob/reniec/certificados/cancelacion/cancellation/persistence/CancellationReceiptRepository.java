package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CancellationReceiptRepository extends JpaRepository<CancellationReceiptEntity, UUID> {

	Optional<CancellationReceiptEntity> findFirstByRequest_IdAndGenerationStatusOrderByAvailableAtDesc(
			UUID requestId, ReceiptGenerationStatus status);
}
