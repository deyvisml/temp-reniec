package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateCancellationRequestRepository
		extends JpaRepository<CertificateCancellationRequestEntity, UUID> {

	Optional<CertificateCancellationRequestEntity> findByDniLookupHashAndLifecycleStatus(
			String dniLookupHash, RequestLifecycleStatus lifecycleStatus);

	Optional<CertificateCancellationRequestEntity> findFirstByDniLookupHashOrderByCreatedAtDesc(
			String dniLookupHash);

	List<CertificateCancellationRequestEntity> findByLifecycleStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
			RequestLifecycleStatus lifecycleStatus, Instant cutoff);
}
