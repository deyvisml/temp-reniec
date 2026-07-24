package pe.gob.reniec.certificados.cancelacion.cancellation.certificates;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import pe.gob.reniec.certificados.cancelacion.cancellation.certificates.CertificateListingException.Reason;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestCertificateEntity;

@Service
public class CertificateListingService {

	private final CertificateListingPort provider;
	private final CertificateListingProperties properties;
	private final CertificateListingPersistenceCoordinator persistence;

	public CertificateListingService(CertificateListingPort provider,
			CertificateListingProperties properties,
			CertificateListingPersistenceCoordinator persistence) {
		this.provider = provider;
		this.properties = properties;
		this.persistence = persistence;
	}

	public CertificateListResponse list(Long requestId, String correlationId) {
		CertificateListingPersistenceCoordinator.Preparation preparation = persistence.prepare(
				requestId, correlationId, properties.getStaleReservationThreshold());
		if (!preparation.providerRequired()) return response(preparation.snapshot());
		try {
			CertificateListingResult result = provider.listCertificates(preparation.dni(), correlationId);
			if (result == null || result.outcome() == null) {
				throw new CertificateListingException(Reason.INVALID_PROVIDER_RESPONSE,
						"Certificate provider returned no normalized outcome");
			}
			if (result.outcome() != CertificateListingResult.Outcome.SUCCESS) {
				throw providerFailure(result);
			}
			List<CertificateListingResult.ListedCertificate> normalized = validateAndNormalize(result.certificates());
			return response(persistence.complete(requestId, normalized, correlationId));
		}
		catch (CertificateListingException exception) {
			persistence.restoreAfterFailure(requestId, exception.reason().name(), correlationId);
			throw exception;
		}
		catch (RuntimeException exception) {
			persistence.restoreAfterFailure(requestId, "UNEXPECTED_PROVIDER_FAILURE", correlationId);
			throw new CertificateListingException(Reason.INVALID_PROVIDER_RESPONSE,
					"Certificate provider response could not be processed");
		}
	}

	public CertificateListResponse select(Long requestId, List<String> submittedUuids, String correlationId) {
		if (submittedUuids == null || submittedUuids.isEmpty()) {
			throw new CertificateListingException(Reason.INVALID_SELECTION,
					"At least one certificate is required");
		}
		Set<String> normalized = new HashSet<>();
		for (String value : submittedUuids) {
			String uuid = canonicalUuid(value);
			if (!normalized.add(uuid)) {
				throw new CertificateListingException(Reason.INVALID_SELECTION,
						"Duplicate certificate UUID in selection");
			}
		}
		return response(persistence.replaceSelection(requestId, Set.copyOf(normalized), correlationId));
	}

	private static List<CertificateListingResult.ListedCertificate> validateAndNormalize(
			List<CertificateListingResult.ListedCertificate> listed) {
		if (listed == null) {
			throw new CertificateListingException(Reason.INVALID_PROVIDER_RESPONSE,
					"Certificate collection is missing");
		}
		Set<String> uuids = new HashSet<>();
		Set<String> orders = new HashSet<>();
		Instant now = Instant.now();
		return listed.stream().map(item -> {
			if (item == null || item.orderNumber() == null || item.orderNumber().isBlank()
					|| item.orderNumber().trim().length() > 64
					|| !StandardCharsets.US_ASCII.newEncoder().canEncode(item.orderNumber().trim())
					|| item.emissionCreatedAt() == null || item.emissionCreatedAt().isAfter(now)) {
				throw new CertificateListingException(Reason.INVALID_PROVIDER_RESPONSE,
						"Certificate provider returned invalid required data");
			}
			String order = item.orderNumber().trim();
			String uuid;
			try { uuid = canonicalUuid(item.certificateUuid()); }
			catch (CertificateListingException exception) {
				throw new CertificateListingException(Reason.INVALID_PROVIDER_RESPONSE,
						"Certificate provider returned an invalid UUID");
			}
			if (!orders.add(order) || !uuids.add(uuid)) {
				throw new CertificateListingException(Reason.INVALID_PROVIDER_RESPONSE,
						"Certificate provider returned duplicate data");
			}
			return new CertificateListingResult.ListedCertificate(order, item.emissionCreatedAt(), uuid);
		}).toList();
	}

	private static CertificateListingException providerFailure(CertificateListingResult result) {
		Reason reason = switch (result.outcome()) {
			case TIMEOUT -> Reason.TIMEOUT;
			case UNAVAILABLE -> Reason.UNAVAILABLE;
			case MALFORMED -> Reason.INVALID_PROVIDER_RESPONSE;
			case SUCCESS -> throw new IllegalStateException("Success is not a failure");
		};
		return new CertificateListingException(reason, "Certificate provider failed: " + result.errorCode());
	}

	private static String canonicalUuid(String value) {
		if (value == null) throw new CertificateListingException(Reason.INVALID_SELECTION, "UUID is required");
		String normalized = value.toLowerCase(Locale.ROOT);
		try {
			UUID parsed = UUID.fromString(normalized);
			if (!parsed.toString().equals(normalized)) throw new IllegalArgumentException();
			return normalized;
		}
		catch (IllegalArgumentException exception) {
			throw new CertificateListingException(Reason.INVALID_SELECTION, "UUID must be canonical");
		}
	}

	private static CertificateListResponse response(List<CancellationRequestCertificateEntity> entities) {
		List<CertificateListResponse.CertificateItem> items = entities.stream()
				.map(entity -> new CertificateListResponse.CertificateItem(entity.getOrderNumber(),
						entity.getEmissionCreatedAt(), entity.getCertificateUuid(),
						entity.getAvailabilityStatus().name(), entity.isSelected()))
				.toList();
		int selected = (int) entities.stream().filter(CancellationRequestCertificateEntity::isSelected).count();
		String status = entities.isEmpty() ? "NO_CERTIFICATES_AVAILABLE"
				: selected > 0 ? "CERTIFICATES_SELECTED" : "CERTIFICATES_AVAILABLE";
		return new CertificateListResponse(status, items, selected, selected > 0);
	}
}
