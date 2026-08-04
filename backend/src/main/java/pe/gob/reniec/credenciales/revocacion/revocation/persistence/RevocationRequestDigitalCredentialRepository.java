package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RevocationRequestDigitalCredentialRepository
		extends JpaRepository<RevocationRequestDigitalCredentialEntity, Long> {

	List<RevocationRequestDigitalCredentialEntity> findByRequest_IdOrderByEmissionCreatedAtAscIdAsc(Long requestId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select digitalCredential from RevocationRequestDigitalCredentialEntity digitalCredential "
			+ "where digitalCredential.request.id = :requestId order by digitalCredential.emissionCreatedAt asc, digitalCredential.id asc")
	List<RevocationRequestDigitalCredentialEntity> findByRequestIdForUpdate(@Param("requestId") Long requestId);

	Optional<RevocationRequestDigitalCredentialEntity> findByRequest_IdAndDigitalCredentialUuidAndStatusListIndex(
			Long requestId, String digitalCredentialUuid, Integer statusListIndex);

	List<RevocationRequestDigitalCredentialEntity> findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(
			Long requestId);

	long countByRequest_Id(Long requestId);

	long countByRequest_IdAndSelectedTrue(Long requestId);
}
