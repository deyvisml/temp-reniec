package pe.gob.reniec.credenciales.revocacion.revocation.persistence;

import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdentityVerificationRepository extends JpaRepository<IdentityVerificationEntity, Long> {

	@EntityGraph(attributePaths = "request")
	Optional<IdentityVerificationEntity> findFirstByRequest_IdOrderByAttemptNumberDesc(Long requestId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = "request")
	Optional<IdentityVerificationEntity> findTopByRequest_IdOrderByAttemptNumberDesc(Long requestId);

	Optional<IdentityVerificationEntity> findFirstByRequest_IdAndVerificationStatusOrderByAttemptNumberDesc(
			Long requestId, IdentityVerificationStatus status);

	Optional<IdentityVerificationEntity> findByStateHash(String stateHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select verification from IdentityVerificationEntity verification where verification.id = :id")
	Optional<IdentityVerificationEntity> findByIdForUpdate(@Param("id") Long id);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update IdentityVerificationEntity verification
			set verification.stateConsumedAt = :consumedAt
			where verification.stateHash = :stateHash
			  and verification.verificationStatus = pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationStatus.STARTED
			  and verification.stateConsumedAt is null
			  and verification.stateExpiresAt >= :consumedAt
			""")
	int consumeState(@Param("stateHash") String stateHash, @Param("consumedAt") Instant consumedAt);
}
