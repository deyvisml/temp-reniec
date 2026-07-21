package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CancellationRequestCertificateRepository
		extends JpaRepository<CancellationRequestCertificateEntity, Long> {

	List<CancellationRequestCertificateEntity> findByRequest_IdOrderByEmissionCreatedAtAscIdAsc(Long requestId);

	Optional<CancellationRequestCertificateEntity> findByRequest_IdAndCertificateUuid(
			Long requestId, String certificateUuid);

	List<CancellationRequestCertificateEntity> findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(
			Long requestId);

	long countByRequest_Id(Long requestId);

	long countByRequest_IdAndSelectedTrue(Long requestId);
}
