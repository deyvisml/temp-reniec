package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CancellationReceiptRepository extends JpaRepository<CancellationReceiptEntity, Long> {

	Optional<CancellationReceiptEntity> findFirstByRequest_IdAndGenerationStatusOrderByAvailableAtDesc(
			Long requestId, ReceiptGenerationStatus status);
}
