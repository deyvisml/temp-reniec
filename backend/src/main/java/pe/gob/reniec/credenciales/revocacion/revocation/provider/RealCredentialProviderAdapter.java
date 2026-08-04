package pe.gob.reniec.credenciales.revocacion.revocation.provider;

import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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
	private static final int MAX_DIAGNOSTIC_PAYLOAD_LENGTH = 16_000;
	private static final ZoneId PROVIDER_ZONE = ZoneId.of("America/Lima");

	private final CredentialProviderProperties properties;
	private final RestClient client;
	private final Clock clock;
	private final ObjectMapper objectMapper;

	@Autowired
	public RealCredentialProviderAdapter(CredentialProviderProperties properties) {
		this(properties, Clock.systemUTC());
	}

	RealCredentialProviderAdapter(CredentialProviderProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.objectMapper = new ObjectMapper();
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
		LOGGER.info("Credential provider request operation=list-credentials endpoint={} correlationId={}",
				LIST_CREDENTIALS_PATH, correlationId);
		try {
			ResponseEntity<String> providerResponse = postEntity(LIST_CREDENTIALS_PATH,
					new DniRequest(dni), String.class);
			String responseBody = providerResponse.getBody();
			LOGGER.info("Credential provider response operation=list-credentials httpStatus={} contentType={} bodyLength={}",
					providerResponse.getStatusCode().value(), providerResponse.getHeaders().getContentType(),
					responseBody == null ? 0 : responseBody.length());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Credential provider payload operation=list-credentials responseBody={}",
						diagnosticPayload(responseBody));
			}
			if (responseBody == null || responseBody.isBlank()) {
				logListingFailure(DigitalCredentialListingResult.Outcome.MALFORMED,
						ListingDiagnostic.EMPTY_RESPONSE_BODY, "None", null, null);
				return listingFailure(DigitalCredentialListingResult.Outcome.MALFORMED,
						"PROVIDER_INVALID_RESPONSE");
			}
			ProviderCredential[] response = objectMapper.readValue(responseBody, ProviderCredential[].class);
			LOGGER.info("Credential provider parsed operation=list-credentials credentialCount={}", response.length);
			List<DigitalCredentialListingResult.ListedDigitalCredential> listed = new ArrayList<>(response.length);
			for (int position = 0; position < response.length; position++) {
				ProviderCredential item = response[position];
				logProviderCredential(position, item);
				listed.add(normalize(item, position));
			}
			LOGGER.info("Credential provider validation operation=list-credentials outcome=SUCCESS credentialCount={}",
					listed.size());
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
			logListingFailure(outcome, diagnostic, exception.getClass().getSimpleName(), null, null);
			return listingFailure(outcome, timeout ? "PROVIDER_TIMEOUT" : "PROVIDER_UNAVAILABLE");
		}
		catch (RestClientResponseException exception) {
			String responseBody = exception.getResponseBodyAsString();
			LOGGER.warn("Credential provider HTTP error operation=list-credentials httpStatus={} contentType={} bodyLength={} exceptionType={}",
					exception.getStatusCode().value(), exception.getResponseHeaders() == null ? null
							: exception.getResponseHeaders().getContentType(), responseBody.length(),
					exception.getClass().getSimpleName());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Credential provider error payload operation=list-credentials responseBody={}",
						diagnosticPayload(responseBody));
			}
			return listingFailure(DigitalCredentialListingResult.Outcome.MALFORMED,
					"PROVIDER_INVALID_RESPONSE");
		}
		catch (InvalidProviderCredentialException exception) {
			logListingFailure(DigitalCredentialListingResult.Outcome.MALFORMED,
					exception.diagnostic(), exception.getClass().getSimpleName(),
					exception.credentialPosition(), exception.field());
			return listingFailure(DigitalCredentialListingResult.Outcome.MALFORMED,
					"PROVIDER_INVALID_RESPONSE");
		}
		catch (JsonProcessingException exception) {
			LOGGER.warn("Credential provider parsing failed operation=list-credentials diagnostic={} line={} column={} exceptionType={}",
					ListingDiagnostic.INVALID_JSON_OR_STRUCTURE,
					exception.getLocation() == null ? null : exception.getLocation().getLineNr(),
					exception.getLocation() == null ? null : exception.getLocation().getColumnNr(),
					exception.getClass().getSimpleName());
			return listingFailure(DigitalCredentialListingResult.Outcome.MALFORMED,
					"PROVIDER_INVALID_RESPONSE");
		}
		catch (RuntimeException exception) {
			logListingFailure(DigitalCredentialListingResult.Outcome.MALFORMED,
					ListingDiagnostic.INVALID_JSON_OR_STRUCTURE, exception.getClass().getSimpleName(), null, null);
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

	private DigitalCredentialListingResult.ListedDigitalCredential normalize(ProviderCredential item, int position) {
		if (item == null) throw invalid(ListingDiagnostic.MISSING_REQUIRED_DATA, position, "item");
		if (item.statusListIndex() == null || item.statusListIndex() < 0) {
			throw invalid(ListingDiagnostic.MISSING_REQUIRED_DATA, position, "statusListIndex");
		}
		if (item.credentialStatus() == null) {
			throw invalid(ListingDiagnostic.MISSING_REQUIRED_DATA, position, "credentialStatus");
		}
		if (item.credentialType() == null || item.credentialType().isBlank()) {
			throw invalid(ListingDiagnostic.MISSING_REQUIRED_DATA, position, "credentialType");
		}
		DigitalCredentialStatus status = switch (item.credentialStatus()) {
			case 0 -> DigitalCredentialStatus.ACTIVE;
			case 1 -> DigitalCredentialStatus.REVOKED;
			default -> throw invalid(ListingDiagnostic.UNKNOWN_CREDENTIAL_STATUS, position, "credentialStatus");
		};
		Instant issuedAt = providerInstant(item.issuanceDate(), position, "issuanceDate");
		Instant revokedAt = null;
		if (status == DigitalCredentialStatus.REVOKED) {
			revokedAt = providerRevocationInstant(item.revocateDate(), position, item.statusListIndex());
		}
		return new DigitalCredentialListingResult.ListedDigitalCredential(item.statusListIndex(),
				item.credentialType().trim(), issuedAt, item.listCredential(), status, revokedAt,
				item.credentialStatus());
	}

	private Instant providerInstant(String value, int position, String field) {
		if (value == null || value.isBlank()) {
			throw invalid(ListingDiagnostic.MISSING_REQUIRED_DATA, position, field);
		}
		try {
			return LocalDateTime.parse(value).atZone(PROVIDER_ZONE).toInstant();
		}
		catch (DateTimeParseException exception) {
			throw invalid(ListingDiagnostic.INVALID_PROVIDER_DATE, position, field);
		}
	}

	private Instant providerRevocationInstant(String value, int position, int statusListIndex) {
		if (value == null || value.isBlank()) {
			logDiscardedRevocationDate(position, statusListIndex, RevocationDateDiscardReason.MISSING);
			return null;
		}
		try {
			return LocalDateTime.parse(value).atZone(PROVIDER_ZONE).toInstant();
		}
		catch (DateTimeParseException exception) {
			logDiscardedRevocationDate(position, statusListIndex, RevocationDateDiscardReason.INVALID_FORMAT);
			return null;
		}
	}

	private static void logDiscardedRevocationDate(int position, int statusListIndex,
			RevocationDateDiscardReason reason) {
		LOGGER.warn("Credential provider revocation date discarded operation=list-credentials credentialPosition={} statusListIndex={} reason={}",
				position, statusListIndex, reason);
	}

	private static InvalidProviderCredentialException invalid(ListingDiagnostic diagnostic,
			Integer credentialPosition, String field) {
		return new InvalidProviderCredentialException(diagnostic, credentialPosition, field);
	}

	private static void logListingFailure(DigitalCredentialListingResult.Outcome outcome,
			ListingDiagnostic diagnostic, String exceptionType, Integer credentialPosition, String field) {
		LOGGER.warn("Credential provider operation=list-credentials outcome={} diagnostic={} credentialPosition={} field={} exceptionType={}",
				outcome, diagnostic, credentialPosition, field, exceptionType);
	}

	private static void logProviderCredential(int position, ProviderCredential item) {
		if (item == null) {
			LOGGER.info("Credential provider item operation=list-credentials credentialPosition={} item=null", position);
			return;
		}
		LOGGER.info("Credential provider item operation=list-credentials credentialPosition={} statusListIndex={} credentialStatus={} credentialType={} issuanceDatePresent={} revocateDatePresent={} activeRevocationDateIgnored={} listCredential={}",
				position, item.statusListIndex(), item.credentialStatus(), item.credentialType(),
				hasText(item.issuanceDate()), hasText(item.revocateDate()),
				Integer.valueOf(0).equals(item.credentialStatus()) && hasText(item.revocateDate()),
				maskedCredential(item.listCredential()));
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private static String maskedCredential(String value) {
		if (value == null || value.isBlank()) return String.valueOf(value);
		return value.length() <= 4 ? "****" : "****" + value.substring(value.length() - 4);
	}

	private static String diagnosticPayload(String value) {
		if (value == null) return null;
		String singleLine = value.replace("\r", "\\r").replace("\n", "\\n");
		if (singleLine.length() <= MAX_DIAGNOSTIC_PAYLOAD_LENGTH) return singleLine;
		return singleLine.substring(0, MAX_DIAGNOSTIC_PAYLOAD_LENGTH) + "...[truncated]";
	}

	private <T> T post(String path, Object body, Class<T> responseType) {
		return client.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
				.retrieve().body(responseType);
	}

	private <T> ResponseEntity<T> postEntity(String path, Object body, Class<T> responseType) {
		return client.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
				.retrieve().toEntity(responseType);
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
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ProviderCredential(String credentialType, String listCredential,
			Integer statusListIndex, String issuanceDate, String revocateDate,
			Integer credentialStatus) { }
	private record ProviderRevocationRequest(String listCredential, Integer statusListIndex,
			@JsonProperty("cui_dni") String dni) { }
	private record ProviderRevocationResponse(Integer credentialStatus) { }

	private enum ListingDiagnostic {
		EMPTY_RESPONSE_BODY,
		UNKNOWN_CREDENTIAL_STATUS,
		INVALID_PROVIDER_DATE,
		MISSING_REQUIRED_DATA,
		INVALID_JSON_OR_STRUCTURE,
		PROVIDER_TIMEOUT,
		PROVIDER_UNAVAILABLE
	}

	private enum RevocationDateDiscardReason {
		MISSING,
		INVALID_FORMAT
	}

	private static final class InvalidProviderCredentialException extends RuntimeException {
		private final ListingDiagnostic diagnostic;
		private final Integer credentialPosition;
		private final String field;

		private InvalidProviderCredentialException(ListingDiagnostic diagnostic,
				Integer credentialPosition, String field) {
			super(diagnostic.name());
			this.diagnostic = diagnostic;
			this.credentialPosition = credentialPosition;
			this.field = field;
		}

		private ListingDiagnostic diagnostic() { return diagnostic; }
		private Integer credentialPosition() { return credentialPosition; }
		private String field() { return field; }
	}
}
