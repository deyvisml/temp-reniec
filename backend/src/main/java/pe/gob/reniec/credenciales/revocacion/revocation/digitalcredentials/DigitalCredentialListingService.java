package pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialListingException.Reason;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestDigitalCredentialEntity;

@Service
public class DigitalCredentialListingService {
	private static final Logger LOGGER = LoggerFactory.getLogger(DigitalCredentialListingService.class);

	private final DigitalCredentialListingPort provider;
	private final DigitalCredentialListingProperties properties;
	private final DigitalCredentialListingPersistenceCoordinator persistence;

	public DigitalCredentialListingService(DigitalCredentialListingPort provider,
			DigitalCredentialListingProperties properties,
			DigitalCredentialListingPersistenceCoordinator persistence) {
		this.provider = provider;
		this.properties = properties;
		this.persistence = persistence;
	}

	public DigitalCredentialListResponse list(Long requestId, String correlationId) {
		DigitalCredentialListingPersistenceCoordinator.Preparation preparation = persistence.prepare(
				requestId, correlationId, properties.getStaleReservationThreshold());
		if (!preparation.providerRequired()) {
			return response(preparation.snapshot(), preparation.requestStatus().name());
		}
		try {
			List<DigitalCredentialListingResult.ListedDigitalCredential> normalized =
					fetchValidated(preparation.dni(), correlationId);
			return response(persistence.complete(requestId, normalized, correlationId));
		}
		catch (DigitalCredentialListingException exception) {
			persistence.restoreAfterFailure(requestId, preparation.previousStatus(),
					exception.reason().name(), correlationId);
			throw exception;
		}
		catch (RuntimeException exception) {
			persistence.restoreAfterFailure(requestId, preparation.previousStatus(),
					"UNEXPECTED_PROVIDER_FAILURE", correlationId);
			throw new DigitalCredentialListingException(Reason.INVALID_PROVIDER_RESPONSE,
					"DigitalCredential provider response could not be processed");
		}
	}

	public RevalidationOutcome revalidateSelection(Long requestId, String submittedUuid,
			int submittedStatusListIndex, String correlationId) {
		String uuid = canonicalUuid(submittedUuid);
		if (submittedStatusListIndex < 0) {
			throw new DigitalCredentialListingException(Reason.INVALID_SELECTION,
					"DigitalCredential statusListIndex must not be negative");
		}
		DigitalCredentialListingPersistenceCoordinator.Preparation preparation = persistence.prepare(
				requestId, correlationId, properties.getStaleReservationThreshold());
		if (!preparation.providerRequired()) return RevalidationOutcome.FROZEN;
		try {
			List<DigitalCredentialListingResult.ListedDigitalCredential> normalized =
					fetchValidated(preparation.dni(), correlationId);
			DigitalCredentialListingPersistenceCoordinator.RevalidationCompletion completion =
					persistence.completeForConfirmation(requestId, normalized, uuid,
							submittedStatusListIndex, correlationId);
			return completion.selectedIsCurrent() ? RevalidationOutcome.CURRENT : RevalidationOutcome.STALE;
		}
		catch (DigitalCredentialListingException exception) {
			persistence.restoreAfterFailure(requestId, preparation.previousStatus(),
					exception.reason().name(), correlationId);
			throw exception;
		}
		catch (RuntimeException exception) {
			persistence.restoreAfterFailure(requestId, preparation.previousStatus(),
					"UNEXPECTED_PROVIDER_FAILURE", correlationId);
			throw new DigitalCredentialListingException(Reason.INVALID_PROVIDER_RESPONSE,
					"DigitalCredential provider response could not be processed");
		}
	}

	private List<DigitalCredentialListingResult.ListedDigitalCredential> fetchValidated(
			String dni, String correlationId) {
		DigitalCredentialListingResult result = provider.listDigitalCredentials(dni, correlationId);
		if (result == null || result.outcome() == null) {
			throw new DigitalCredentialListingException(Reason.INVALID_PROVIDER_RESPONSE,
					"DigitalCredential provider returned no normalized outcome");
		}
		if (result.outcome() != DigitalCredentialListingResult.Outcome.SUCCESS) {
			throw providerFailure(result);
		}
		return validateAndNormalize(result.digitalCredentials());
	}

	private static List<DigitalCredentialListingResult.ListedDigitalCredential> validateAndNormalize(
			List<DigitalCredentialListingResult.ListedDigitalCredential> listed) {
		if (listed == null) {
			throw new DigitalCredentialListingException(Reason.INVALID_PROVIDER_RESPONSE,
					"DigitalCredential collection is missing");
		}
		Set<Integer> indexes = new HashSet<>();
		Set<CredentialIdentity> identities = new HashSet<>();
		Instant now = Instant.now();
		List<DigitalCredentialListingResult.ListedDigitalCredential> normalized = listed.stream().map(item -> {
			if (item == null || item.statusListIndex() < 0 || item.credentialType() == null
					|| item.credentialType().isBlank() || item.credentialType().trim().length() > 100
					|| item.emissionCreatedAt() == null || item.emissionCreatedAt().isAfter(now)
					|| item.status() == null || item.providerCredentialStatus() < 0
					|| item.providerCredentialStatus() > 1) {
				throw new DigitalCredentialListingException(Reason.INVALID_PROVIDER_RESPONSE,
						"DigitalCredential provider returned invalid required data");
			}
			if ((item.status() == DigitalCredentialStatus.ACTIVE && item.providerCredentialStatus() != 0)
					|| (item.status() == DigitalCredentialStatus.REVOKED && item.providerCredentialStatus() != 1)) {
				throw new DigitalCredentialListingException(Reason.INVALID_PROVIDER_RESPONSE,
						"DigitalCredential provider returned an inconsistent status");
			}
			String uuid;
			try { uuid = canonicalUuid(item.digitalCredentialUuid()); }
			catch (DigitalCredentialListingException exception) {
				throw new DigitalCredentialListingException(Reason.INVALID_PROVIDER_RESPONSE,
						"DigitalCredential provider returned an invalid UUID");
			}
			CredentialIdentity identity = new CredentialIdentity(uuid, item.statusListIndex());
			if (!identities.add(identity) || !indexes.add(item.statusListIndex())) {
				throw new DigitalCredentialListingException(Reason.INVALID_PROVIDER_RESPONSE,
						"DigitalCredential provider returned duplicate data");
			}
			Instant revokedAt = normalizedRevocationDate(item);
			return new DigitalCredentialListingResult.ListedDigitalCredential(item.statusListIndex(),
					item.credentialType().trim(), item.emissionCreatedAt(), uuid,
					item.status(), revokedAt, item.providerCredentialStatus());
		}).toList();
		return normalized;
	}

	private static Instant normalizedRevocationDate(
			DigitalCredentialListingResult.ListedDigitalCredential item) {
		if (item.status() == DigitalCredentialStatus.ACTIVE || item.revokedAt() == null) return null;
		if (item.revokedAt().isBefore(item.emissionCreatedAt())) {
			logDiscardedRevocationDate(item.statusListIndex(), RevocationDateDiscardReason.BEFORE_ISSUANCE);
			return null;
		}
		return item.revokedAt();
	}

	private static void logDiscardedRevocationDate(int statusListIndex,
			RevocationDateDiscardReason reason) {
		LOGGER.warn("Credential provider revocation date discarded operation=list-credentials statusListIndex={} reason={}",
				statusListIndex, reason);
	}

	private enum RevocationDateDiscardReason {
		BEFORE_ISSUANCE
	}

	private record CredentialIdentity(String digitalCredentialUuid, int statusListIndex) { }

	public enum RevalidationOutcome {
		CURRENT,
		STALE,
		FROZEN
	}

	private static DigitalCredentialListingException providerFailure(DigitalCredentialListingResult result) {
		Reason reason = switch (result.outcome()) {
			case TIMEOUT -> Reason.TIMEOUT;
			case UNAVAILABLE -> Reason.UNAVAILABLE;
			case MALFORMED -> Reason.INVALID_PROVIDER_RESPONSE;
			case SUCCESS -> throw new IllegalStateException("Success is not a failure");
		};
		return new DigitalCredentialListingException(reason, "DigitalCredential provider failed: " + result.errorCode());
	}

	private static String canonicalUuid(String value) {
		if (value == null) throw new DigitalCredentialListingException(Reason.INVALID_SELECTION, "UUID is required");
		String normalized = value.toLowerCase(Locale.ROOT);
		try {
			UUID parsed = UUID.fromString(normalized);
			if (!parsed.toString().equals(normalized)) throw new IllegalArgumentException();
			return normalized;
		}
		catch (IllegalArgumentException exception) {
			throw new DigitalCredentialListingException(Reason.INVALID_SELECTION, "UUID must be canonical");
		}
	}

	private static DigitalCredentialListResponse response(List<RevocationRequestDigitalCredentialEntity> entities) {
		String status = entities.isEmpty() ? "NO_DIGITAL_CREDENTIALS_AVAILABLE"
				: entities.stream().anyMatch(RevocationRequestDigitalCredentialEntity::isSelected)
						? "CONFIRMED" : "DIGITAL_CREDENTIALS_AVAILABLE";
		return response(entities, status);
	}

	private static DigitalCredentialListResponse response(List<RevocationRequestDigitalCredentialEntity> entities,
			String requestStatus) {
		List<DigitalCredentialListResponse.DigitalCredentialItem> items = entities.stream()
				.map(entity -> new DigitalCredentialListResponse.DigitalCredentialItem(entity.getStatusListIndex(),
						entity.getEmissionCreatedAt(), entity.getDigitalCredentialUuid(),
						publicStatus(entity), entity.getRevokedAt(), entity.isSelected()))
				.toList();
		boolean hasActive = entities.stream().anyMatch(entity ->
				entity.getAvailabilityStatus() == pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialAvailabilityStatus.AVAILABLE);
		return new DigitalCredentialListResponse(requestStatus, items, hasActive);
	}

	private static DigitalCredentialStatus publicStatus(RevocationRequestDigitalCredentialEntity entity) {
		return switch (entity.getAvailabilityStatus()) {
			case AVAILABLE -> DigitalCredentialStatus.ACTIVE;
			case REVOKED -> DigitalCredentialStatus.REVOKED;
			default -> throw new IllegalStateException("DigitalCredential snapshot contains a non-listable status");
		};
	}
}
