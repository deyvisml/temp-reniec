package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface DigitalCredentialAvailabilityCheckRepository extends JpaRepository<DigitalCredentialAvailabilityCheckEntity, Long> {

	Optional<DigitalCredentialAvailabilityCheckEntity> findFirstByRequest_IdOrderByAttemptNumberDesc(Long requestId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select check from DigitalCredentialAvailabilityCheckEntity check where check.id = :id")
	Optional<DigitalCredentialAvailabilityCheckEntity> findByIdForUpdate(@Param("id") Long id);
}
