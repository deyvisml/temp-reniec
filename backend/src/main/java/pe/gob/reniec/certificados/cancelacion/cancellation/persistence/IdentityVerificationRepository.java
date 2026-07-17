package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityVerificationRepository extends JpaRepository<IdentityVerificationEntity, Long> {

	Optional<IdentityVerificationEntity> findFirstByRequest_IdOrderByAttemptNumberDesc(Long requestId);

	Optional<IdentityVerificationEntity> findFirstByRequest_IdAndVerificationStatusOrderByAttemptNumberDesc(
			Long requestId, IdentityVerificationStatus status);
}
