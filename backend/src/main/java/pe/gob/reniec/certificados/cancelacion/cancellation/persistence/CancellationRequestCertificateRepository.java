package pe.gob.reniec.certificados.cancelacion.cancellation.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CancellationRequestCertificateRepository
		extends JpaRepository<CancellationRequestCertificateEntity, Long> {

	List<CancellationRequestCertificateEntity> findByRequest_IdOrderByEmissionCreatedAtAscIdAsc(Long requestId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select certificate from CancellationRequestCertificateEntity certificate "
			+ "where certificate.request.id = :requestId order by certificate.emissionCreatedAt asc, certificate.id asc")
	List<CancellationRequestCertificateEntity> findByRequestIdForUpdate(@Param("requestId") Long requestId);

	Optional<CancellationRequestCertificateEntity> findByRequest_IdAndCertificateUuid(
			Long requestId, String certificateUuid);

	List<CancellationRequestCertificateEntity> findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(
			Long requestId);

	long countByRequest_Id(Long requestId);

	long countByRequest_IdAndSelectedTrue(Long requestId);
}
