package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CertificateEligibilityCheckRepository extends JpaRepository<CertificateEligibilityCheckEntity, Long> {

	Optional<CertificateEligibilityCheckEntity> findFirstByRequest_IdOrderByAttemptNumberDesc(Long requestId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select check from CertificateEligibilityCheckEntity check where check.id = :id")
	Optional<CertificateEligibilityCheckEntity> findByIdForUpdate(@Param("id") Long id);
}
