package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RevocationOperationRepository extends JpaRepository<RevocationOperationEntity, Long> {

	Optional<RevocationOperationEntity> findByIdempotencyKey(String idempotencyKey);

	Optional<RevocationOperationEntity> findFirstByRequest_IdOrderByAttemptNumberDesc(Long requestId);

	Optional<RevocationOperationEntity> findFirstByRequest_IdAndOperationStatusInOrderByAttemptNumberDesc(
			Long requestId, Collection<RevocationOperationStatus> statuses);

	@Query("""
			select operation.request.id
			from RevocationOperationEntity operation
			where operation.operationStatus = pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationStatus.SUCCEEDED
			and not exists (
				select receipt.id from RevocationReceiptEntity receipt
				where receipt.request.id = operation.request.id
			)
			order by operation.completedAt asc
			""")
	List<Long> findSuccessfulRequestIdsWithoutReceipt(Pageable pageable);
}
