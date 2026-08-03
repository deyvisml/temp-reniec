package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RevocationReceiptRepository extends JpaRepository<RevocationReceiptEntity, Long> {

	Optional<RevocationReceiptEntity> findFirstByRequest_IdOrderByCreatedAtDesc(Long requestId);

	Optional<RevocationReceiptEntity> findFirstByRequest_IdAndGenerationStatusOrderByAvailableAtDesc(
			Long requestId, ReceiptGenerationStatus status);

	@Query("""
			select receipt.request.id
			from RevocationReceiptEntity receipt
			where receipt.generationStatus in :statuses
			order by receipt.updatedAt asc
			""")
	List<Long> findGenerationCandidateRequestIds(
			@Param("statuses") Collection<ReceiptGenerationStatus> statuses, Pageable pageable);
}
