package pe.gob.reniec.credenciales.revocacion.revocation.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.execution.RevocationGateway;
import pe.gob.reniec.credenciales.revocacion.revocation.initiation.AvailabilityProperties;
import pe.gob.reniec.credenciales.revocacion.revocation.initiation.AvailabilityOutcome;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationResult;

@ExtendWith(OutputCaptureExtension.class)
class RealCredentialProviderAdapterTests {

	private HttpServer server;
	private final List<CapturedRequest> requests = new ArrayList<>();
	private String availabilityResponse;
	private String listingResponse;
	private String revocationResponse;

	@BeforeEach
	void startServer() throws IOException {
		availabilityResponse = "{\"title\":\"ignored\",\"credentials\":true}";
		listingResponse = "[{\"title\":\"ignored\",\"credentialType\":\"DniPeruanoCredential\","
				+ "\"listCredential\":\"e87a7813-880d-4a2d-92f7-4251c841d008\","
				+ "\"statusListIndex\":31,\"issuanceDate\":\"2026-07-31T23:08:16\","
				+ "\"revocateDate\":null,\"credentialStatus\":0}]";
		revocationResponse = "{\"title\":\"ignored\",\"message\":\"already revoked\",\"credentialStatus\":1}";
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/api/v1/has-credentials", exchange -> respond(exchange, availabilityResponse));
		server.createContext("/api/v1/list-credentials", exchange -> respond(exchange, listingResponse));
		server.createContext("/api/v1/revocation", exchange -> respond(exchange, revocationResponse));
		server.start();
	}

	@Test
	void normalizesNegativeAndUncertainProviderOutcomesByCodesOnly() {
		availabilityResponse = "{\"credentials\":false}";
		revocationResponse = "{\"message\":\"ignored\",\"credentialStatus\":0}";
		RealCredentialProviderAdapter adapter = adapter();

		assertThat(adapter.checkAvailability("42992664").outcome()).isEqualTo(AvailabilityOutcome.NOT_AVAILABLE);
		assertThat(adapter.revoke(new RevocationGateway.Command(
				"e87a7813-880d-4a2d-92f7-4251c841d008", 31, "42992664", "operation-1")).outcome())
				.isEqualTo(RevocationResult.FAILED);

		revocationResponse = "{\"credentialStatus\":9}";
		assertThat(adapter.revoke(new RevocationGateway.Command(
				"e87a7813-880d-4a2d-92f7-4251c841d008", 31, "42992664", "operation-1")).outcome())
				.isEqualTo(RevocationResult.OUTCOME_UNKNOWN);
	}

	@Test
	void rejectsUnknownStatusesAndInvalidLimaDatesAsMalformedListings(CapturedOutput output) {
		listingResponse = listingResponse.replace("\"credentialStatus\":0", "\"credentialStatus\":7");
		assertThat(adapter().listDigitalCredentials("42992664", "correlation").outcome())
				.isEqualTo(pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialListingResult.Outcome.MALFORMED);
		assertThat(output).contains("diagnostic=UNKNOWN_CREDENTIAL_STATUS");

		listingResponse = listingResponse.replace("\"credentialStatus\":7", "\"credentialStatus\":1")
				.replace("\"revocateDate\":null", "\"revocateDate\":\"invalid-date\"");
		assertThat(adapter().listDigitalCredentials("42992664", "correlation").outcome())
				.isEqualTo(pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialListingResult.Outcome.MALFORMED);
		assertThat(output).contains("diagnostic=INVALID_PROVIDER_DATE");
	}

	@Test
	void acceptsActiveCredentialWithExternalRevocationDateAndDiscardsThatDate(CapturedOutput output) {
		listingResponse = listingResponse.replace("\"revocateDate\":null",
				"\"revocateDate\":\"2026-08-04T01:16:25\"");

		var result = adapter().listDigitalCredentials("42983609", "correlation");

		assertThat(result.outcome()).isEqualTo(
				pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialListingResult.Outcome.SUCCESS);
		assertThat(result.digitalCredentials()).singleElement().satisfies(credential -> {
			assertThat(credential.status()).isEqualTo(DigitalCredentialStatus.ACTIVE);
			assertThat(credential.revokedAt()).isNull();
			assertThat(credential.providerCredentialStatus()).isZero();
		});
		assertThat(output)
				.doesNotContain("diagnostic=INCONSISTENT_REVOCATION_DATE")
				.doesNotContain("42983609", "e87a7813-880d-4a2d-92f7-4251c841d008",
						"test-key", "2026-08-04T01:16:25", listingResponse);
	}

	@Test
	void rejectsRevokedCredentialWithoutRevocationDate(CapturedOutput output) {
		listingResponse = listingResponse.replace("\"credentialStatus\":0", "\"credentialStatus\":1");

		var result = adapter().listDigitalCredentials("42983609", "correlation");

		assertThat(result.outcome()).isEqualTo(
				pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialListingResult.Outcome.MALFORMED);
		assertThat(output)
				.contains("diagnostic=INCONSISTENT_REVOCATION_DATE")
				.doesNotContain("42983609", "e87a7813-880d-4a2d-92f7-4251c841d008", "test-key");
	}

	@Test
	void logsInvalidJsonWithoutIncludingTheProviderBody(CapturedOutput output) {
		listingResponse = "provider-body-that-must-not-be-logged";

		var result = adapter().listDigitalCredentials("42983609", "correlation");

		assertThat(result.outcome()).isEqualTo(
				pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialListingResult.Outcome.MALFORMED);
		assertThat(output)
				.contains("diagnostic=INVALID_JSON_OR_STRUCTURE")
				.doesNotContain("42983609", "test-key", listingResponse);
	}

	@Test
	void preservesRepeatedUuidsWithDistinctOfficialIndexes() {
		listingResponse = "[{\"credentialType\":\"DniPeruanoCredential\","
				+ "\"listCredential\":\"e87a7813-880d-4a2d-92f7-4251c841d008\","
				+ "\"statusListIndex\":11,\"issuanceDate\":\"2026-07-06T20:02:44\","
				+ "\"revocateDate\":null,\"credentialStatus\":0},{"
				+ "\"credentialType\":\"DniPeruanoCredential\","
				+ "\"listCredential\":\"e87a7813-880d-4a2d-92f7-4251c841d008\","
				+ "\"statusListIndex\":12,\"issuanceDate\":\"2026-07-07T13:45:35\","
				+ "\"revocateDate\":null,\"credentialStatus\":0}]";

		var result = adapter().listDigitalCredentials("42992664", "correlation");

		assertThat(result.digitalCredentials())
				.extracting(item -> item.digitalCredentialUuid() + ":" + item.statusListIndex())
				.containsExactly(
						"e87a7813-880d-4a2d-92f7-4251c841d008:11",
						"e87a7813-880d-4a2d-92f7-4251c841d008:12");
	}

	@AfterEach
	void stopServer() { server.stop(0); }

	@Test
	void usesTheOfficialContractsAndNormalizesWithoutProviderMessages() {
		RealCredentialProviderAdapter adapter = adapter();

		assertThat(adapter.checkAvailability("42992664").outcome()).isEqualTo(AvailabilityOutcome.AVAILABLE);
		var listing = adapter.listDigitalCredentials("42992664", "correlation");
		assertThat(listing.digitalCredentials()).singleElement().satisfies(item -> {
			assertThat(item.digitalCredentialUuid()).isEqualTo("e87a7813-880d-4a2d-92f7-4251c841d008");
			assertThat(item.statusListIndex()).isEqualTo(31);
			assertThat(item.status()).isEqualTo(DigitalCredentialStatus.ACTIVE);
			assertThat(item.emissionCreatedAt()).isEqualTo(Instant.parse("2026-08-01T04:08:16Z"));
		});
		var result = adapter.revoke(new RevocationGateway.Command(
				"e87a7813-880d-4a2d-92f7-4251c841d008", 31, "42992664", "operation-1"));
		assertThat(result.outcome()).isEqualTo(RevocationResult.SUCCEEDED);
		assertThat(result.providerCredentialStatus()).isEqualTo(1);

		assertThat(requests).extracting(CapturedRequest::path).containsExactly(
				"/api/v1/has-credentials", "/api/v1/list-credentials", "/api/v1/revocation");
		assertThat(requests).allSatisfy(request -> {
			assertThat(request.method()).isEqualTo("POST");
			assertThat(request.apiKey()).isEqualTo("test-key");
			assertThat(request.contentType()).startsWith("application/json");
			assertThat(request.accept()).contains("application/json");
		});
		assertThat(requests.get(0).body()).isEqualTo("{\"dni\":\"42992664\"}");
		assertThat(requests.get(1).body()).isEqualTo("{\"dni\":\"42992664\"}");
		assertThat(requests.get(2).body()).isEqualTo(
				"{\"listCredential\":\"e87a7813-880d-4a2d-92f7-4251c841d008\","
				+ "\"statusListIndex\":31,\"cui_dni\":\"42992664\"}");
	}

	private RealCredentialProviderAdapter adapter() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("test");
		CredentialProviderProperties properties = new CredentialProviderProperties(environment,
				new AvailabilityProperties());
		properties.setMode(CredentialProviderProperties.Mode.REAL);
		properties.setBaseUrl(URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
		properties.setApiKey("test-key");
		properties.setConnectTimeout(Duration.ofSeconds(1));
		properties.setReadTimeout(Duration.ofSeconds(1));
		properties.validate();
		return new RealCredentialProviderAdapter(properties,
				Clock.fixed(Instant.parse("2026-08-03T12:00:00Z"), ZoneOffset.UTC));
	}

	private void respond(HttpExchange exchange, String response) throws IOException {
		requests.add(new CapturedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
				exchange.getRequestHeaders().getFirst("x-api-key"),
				exchange.getRequestHeaders().getFirst("Content-Type"),
				exchange.getRequestHeaders().getFirst("Accept"),
				new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
		byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, bytes.length);
		exchange.getResponseBody().write(bytes);
		exchange.close();
	}

	private record CapturedRequest(String method, String path, String apiKey,
			String contentType, String accept, String body) { }
}
