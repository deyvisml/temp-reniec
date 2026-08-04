package pe.gob.reniec.credenciales.revocacion.revocation.provider;

import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.*;
import pe.gob.reniec.credenciales.revocacion.revocation.execution.RevocationGateway;
import pe.gob.reniec.credenciales.revocacion.revocation.initiation.*;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationResult;

@Component
@ConditionalOnProperty(prefix = "app.credential-provider", name = "mode", havingValue = "real")
public final class RealCredentialProviderAdapter implements DigitalCredentialAvailabilityPort,
		DigitalCredentialListingPort, RevocationGateway {

	private static final Logger LOGGER = LoggerFactory.getLogger(RealCredentialProviderAdapter.class);
	private static final String HAS_CREDENTIALS_PATH = "/api/v1/has-credentials";
	private static final String LIST_CREDENTIALS_PATH = "/api/v1/list-credentials";
	private static final String REVOCATION_PATH = "/api/v1/revocation";
	private static final ZoneId PROVIDER_ZONE = ZoneId.of("America/Lima");

	private final CredentialProviderProperties properties;
	private final RestClient client;
	private final Clock clock;

	@Autowired
	public RealCredentialProviderAdapter(CredentialProviderProperties properties) {
		this(properties, Clock.systemUTC());
	}

	RealCredentialProviderAdapter(CredentialProviderProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(properties.getConnectTimeout());
		factory.setReadTimeout(properties.getReadTimeout());
		client = RestClient.builder().requestFactory(factory)
				.baseUrl(properties.getBaseUrl().toString())
				.defaultHeader("x-api-key", properties.getApiKey())
				.defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
				.build();
	}

	@Override
	public AvailabilityResult checkAvailability(String dni) {
		try {
			HasCredentialsResponse response = post(HAS_CREDENTIALS_PATH, new DniRequest(dni), HasCredentialsResponse.class);
			if (response == null || response.credentials() == null) {
				return new AvailabilityResult(AvailabilityOutcome.ERROR, null, "PROVIDER_INVALID_RESPONSE");
			}
			return new AvailabilityResult(response.credentials() ? AvailabilityOutcome.AVAILABLE
					: AvailabilityOutcome.NOT_AVAILABLE, null, null);
		}
		catch (ResourceAccessException exception) {
			return new AvailabilityResult(AvailabilityOutcome.UNAVAILABLE, null,
					hasTimeoutCause(exception) ? "PROVIDER_TIMEOUT" : "PROVIDER_UNAVAILABLE");
		}
		catch (RestClientException exception) {
			return new AvailabilityResult(AvailabilityOutcome.ERROR, null, "PROVIDER_INVALID_RESPONSE");
		}
	}

	@Override
	public DigitalCredentialListingResult listDigitalCredentials(String dni, String correlationId) {
		try {
			ProviderCredential[] response = post(LIST_CREDENTIALS_PATH, new DniRequest(dni), ProviderCredential[].class);
			if (response == null) {
				logListingFailure(DigitalCredentialListingResult.Outcome.MALFORMED,
						ListingDiagnostic.INVALID_JSON_OR_STRUCTURE, "None");
				return listingFailure(DigitalCredentialListingResult.Outcome.MALFORMED,
						"PROVIDER_INVALID_RESPONSE");
			}
			List<DigitalCredentialListingResult.ListedDigitalCredential> listed =
					java.util.Arrays.stream(response).map(this::normalize).toList();
			return new DigitalCredentialListingResult(DigitalCredentialListingResult.Outcome.SUCCESS,
					listed, null, null);
		}
		catch (ResourceAccessException exception) {
			boolean timeout = hasTimeoutCause(exception);
			DigitalCredentialListingResult.Outcome outcome = timeout
					? DigitalCredentialListingResult.Outcome.TIMEOUT
					: DigitalCredentialListingResult.Outcome.UNAVAILABLE;
			ListingDiagnostic diagnostic = timeout ? ListingDiagnostic.PROVIDER_TIMEOUT
					: ListingDiagnostic.PROVIDER_UNAVAILABLE;
			logListingFailure(outcome, diagnostic, exception.getClass().getSimpleName());
			return listingFailure(outcome, timeout ? "PROVIDER_TIMEOUT" : "PROVIDER_UNAVAILABLE");
		}
		catch (InvalidProviderCredentialException exception) {
			logListingFailure(DigitalCredentialListingResult.Outcome.MALFORMED,
					exception.diagnostic(), exception.getClass().getSimpleName());
			return listingFailure(DigitalCredentialListingResult.Outcome.MALFORMED,
					"PROVIDER_INVALID_RESPONSE");
		}
		catch (RuntimeException exception) {
			logListingFailure(DigitalCredentialListingResult.Outcome.MALFORMED,
					ListingDiagnostic.INVALID_JSON_OR_STRUCTURE, exception.getClass().getSimpleName());
			return listingFailure(DigitalCredentialListingResult.Outcome.MALFORMED,
					"PROVIDER_INVALID_RESPONSE");
		}
	}

	@Override
	public Result revoke(Command command) {
		Instant respondedAt = clock.instant();
		try {
			ProviderRevocationResponse response = post(REVOCATION_PATH,
					new ProviderRevocationRequest(command.digitalCredentialUuid(), command.statusListIndex(), command.dni()),
					ProviderRevocationResponse.class);
			if (response == null || response.credentialStatus() == null) {
				return new Result(RevocationResult.OUTCOME_UNKNOWN, null,
						"PROVIDER_INVALID_RESPONSE", respondedAt, null);
			}
			return switch (response.credentialStatus()) {
				case 1 -> new Result(RevocationResult.SUCCEEDED, null, null, respondedAt, 1);
				case 0 -> new Result(RevocationResult.FAILED, null,
						"PROVIDER_CREDENTIAL_STILL_ACTIVE", respondedAt, 0);
				default -> new Result(RevocationResult.OUTCOME_UNKNOWN, null,
						"PROVIDER_INVALID_STATUS", respondedAt, response.credentialStatus());
			};
		}
		catch (RestClientException exception) {
			return new Result(RevocationResult.OUTCOME_UNKNOWN, null,
					hasTimeoutCause(exception) ? "PROVIDER_TIMEOUT" : "PROVIDER_UNAVAILABLE",
					respondedAt, null);
		}
	}

	private DigitalCredentialListingResult.ListedDigitalCredential normalize(ProviderCredential item) {
		if (item == null || item.statusListIndex() == null || item.statusListIndex() < 0
				|| item.credentialStatus() == null || item.credentialType() == null
				|| item.credentialType().isBlank()) throw invalid(ListingDiagnostic.MISSING_REQUIRED_DATA);
		DigitalCredentialStatus status = switch (item.credentialStatus()) {
			case 0 -> DigitalCredentialStatus.ACTIVE;
			case 1 -> DigitalCredentialStatus.REVOKED;
			default -> throw invalid(ListingDiagnostic.UNKNOWN_CREDENTIAL_STATUS);
		};
		Instant issuedAt = providerInstant(item.issuanceDate());
		Instant revokedAt = null;
		if (status == DigitalCredentialStatus.REVOKED) {
			if (item.revocateDate() == null || item.revocateDate().isBlank()) {
				throw invalid(ListingDiagnostic.INCONSISTENT_REVOCATION_DATE);
			}
			revokedAt = providerInstant(item.revocateDate());
		}
		return new DigitalCredentialListingResult.ListedDigitalCredential(item.statusListIndex(),
				item.credentialType().trim(), issuedAt, item.listCredential(), status, revokedAt,
				item.credentialStatus());
	}

	private Instant providerInstant(String value) {
		if (value == null || value.isBlank()) throw invalid(ListingDiagnostic.MISSING_REQUIRED_DATA);
		try {
			return LocalDateTime.parse(value).atZone(PROVIDER_ZONE).toInstant();
		}
		catch (DateTimeParseException exception) {
			throw invalid(ListingDiagnostic.INVALID_PROVIDER_DATE);
		}
	}

	private static InvalidProviderCredentialException invalid(ListingDiagnostic diagnostic) {
		return new InvalidProviderCredentialException(diagnostic);
	}

	private static void logListingFailure(DigitalCredentialListingResult.Outcome outcome,
			ListingDiagnostic diagnostic, String exceptionType) {
		LOGGER.warn("Credential provider operation=list-credentials outcome={} diagnostic={} exceptionType={}",
				outcome, diagnostic, exceptionType);
	}

	private <T> T post(String path, Object body, Class<T> responseType) {
		return client.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
				.retrieve().body(responseType);
	}

	private static DigitalCredentialListingResult listingFailure(
			DigitalCredentialListingResult.Outcome outcome, String code) {
		return new DigitalCredentialListingResult(outcome, List.of(), null, code);
	}

	private static boolean hasTimeoutCause(Throwable throwable) {
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			if (current instanceof SocketTimeoutException
					|| current instanceof java.net.http.HttpTimeoutException) return true;
		}
		return false;
	}

	private record DniRequest(String dni) { }
	private record HasCredentialsResponse(Boolean credentials) { }
	private record ProviderCredential(String credentialType, String listCredential,
			Integer statusListIndex, String issuanceDate, String revocateDate,
			Integer credentialStatus) { }
	private record ProviderRevocationRequest(String listCredential, Integer statusListIndex,
			@JsonProperty("cui_dni") String dni) { }
	private record ProviderRevocationResponse(Integer credentialStatus) { }

	private enum ListingDiagnostic {
		INCONSISTENT_REVOCATION_DATE,
		UNKNOWN_CREDENTIAL_STATUS,
		INVALID_PROVIDER_DATE,
		MISSING_REQUIRED_DATA,
		INVALID_JSON_OR_STRUCTURE,
		PROVIDER_TIMEOUT,
		PROVIDER_UNAVAILABLE
	}

	private static final class InvalidProviderCredentialException extends RuntimeException {
		private final ListingDiagnostic diagnostic;

		private InvalidProviderCredentialException(ListingDiagnostic diagnostic) {
			super(diagnostic.name());
			this.diagnostic = diagnostic;
		}

		private ListingDiagnostic diagnostic() { return diagnostic; }
	}
}
