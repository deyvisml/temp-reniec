package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityVerificationRepository extends JpaRepository<IdentityVerificationEntity, UUID> {

	Optional<IdentityVerificationEntity> findFirstByRequest_IdOrderByAttemptNumberDesc(UUID requestId);

	Optional<IdentityVerificationEntity> findFirstByRequest_IdAndVerificationStatusOrderByAttemptNumberDesc(
			UUID requestId, IdentityVerificationStatus status);
}
