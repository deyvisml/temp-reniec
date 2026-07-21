package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CertificateCancellationRequestRepository
		extends JpaRepository<CertificateCancellationRequestEntity, Long> {

	Optional<CertificateCancellationRequestEntity> findFirstByDniAndRequestStatusInOrderByCreatedAtDesc(
			String dni, Collection<CancellationRequestStatus> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<CertificateCancellationRequestEntity> findTopByDniOrderByCreatedAtDesc(String dni);

	Optional<CertificateCancellationRequestEntity> findFirstByDniOrderByCreatedAtDesc(String dni);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select request from CertificateCancellationRequestEntity request where request.id = :id")
	Optional<CertificateCancellationRequestEntity> findByIdForUpdate(@Param("id") Long id);
}
