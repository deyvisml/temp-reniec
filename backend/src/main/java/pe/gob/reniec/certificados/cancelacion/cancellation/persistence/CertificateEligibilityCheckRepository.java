package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateEligibilityCheckRepository extends JpaRepository<CertificateEligibilityCheckEntity, UUID> {

	Optional<CertificateEligibilityCheckEntity> findFirstByRequest_IdOrderByAttemptNumberDesc(UUID requestId);
}
