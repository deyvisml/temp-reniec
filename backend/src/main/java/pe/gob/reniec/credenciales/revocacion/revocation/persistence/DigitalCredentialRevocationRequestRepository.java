package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface DigitalCredentialRevocationRequestRepository
		extends JpaRepository<DigitalCredentialRevocationRequestEntity, Long> {

	Optional<DigitalCredentialRevocationRequestEntity> findFirstByDniAndRequestStatusInOrderByCreatedAtDesc(
			String dni, Collection<RevocationRequestStatus> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<DigitalCredentialRevocationRequestEntity> findTopByDniOrderByCreatedAtDesc(String dni);

	Optional<DigitalCredentialRevocationRequestEntity> findFirstByDniOrderByCreatedAtDesc(String dni);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select request from DigitalCredentialRevocationRequestEntity request where request.id = :id")
	Optional<DigitalCredentialRevocationRequestEntity> findByIdForUpdate(@Param("id") Long id);
}
