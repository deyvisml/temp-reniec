package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateEligibilityCheckRepository extends JpaRepository<CertificateEligibilityCheckEntity, Long> {

	Optional<CertificateEligibilityCheckEntity> findFirstByRequest_IdOrderByAttemptNumberDesc(Long requestId);
}
