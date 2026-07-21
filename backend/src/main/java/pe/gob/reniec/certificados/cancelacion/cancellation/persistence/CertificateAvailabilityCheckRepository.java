package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CertificateAvailabilityCheckRepository extends JpaRepository<CertificateAvailabilityCheckEntity, Long> {

	Optional<CertificateAvailabilityCheckEntity> findFirstByRequest_IdOrderByAttemptNumberDesc(Long requestId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select check from CertificateAvailabilityCheckEntity check where check.id = :id")
	Optional<CertificateAvailabilityCheckEntity> findByIdForUpdate(@Param("id") Long id);
}
