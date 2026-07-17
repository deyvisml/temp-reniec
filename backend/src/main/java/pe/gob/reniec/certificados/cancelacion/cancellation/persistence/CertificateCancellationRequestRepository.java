package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateCancellationRequestRepository
		extends JpaRepository<CertificateCancellationRequestEntity, Long> {

	Optional<CertificateCancellationRequestEntity> findFirstByDniAndRequestStatusInOrderByCreatedAtDesc(
			String dni, Collection<CancellationRequestStatus> statuses);

	Optional<CertificateCancellationRequestEntity> findFirstByDniOrderByCreatedAtDesc(String dni);

	List<CertificateCancellationRequestEntity> findByRequestStatusInAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
			Collection<CancellationRequestStatus> statuses, Instant cutoff);
}
