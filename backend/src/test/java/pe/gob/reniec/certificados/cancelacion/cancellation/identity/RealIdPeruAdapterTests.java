package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;

class RealIdPeruAdapterTests {
	private HttpServer server;
	private IdPeruProperties properties;
	private RSAKey key;
	private AtomicReference<String> jwksBody;
	private AtomicReference<String> tokenBody;
	private AtomicReference<String> userinfoBody;
	private AtomicReference<String> tokenAccept;
	private AtomicReference<String> userinfoAccept;
	private AtomicInteger tokenStatus;
	private AtomicInteger tokenCalls;
	private AtomicInteger userinfoTransientFailures;
	private AtomicInteger userinfoCalls;
	private AtomicLong tokenDelayMillis;

	@BeforeEach
	void setup() throws Exception {
		key = new RSAKeyGenerator(2048).keyID("test-kid").generate();
		server = HttpServer.create(new InetSocketAddress(0), 0);
		ApplicationUrlProperties urls = new ApplicationUrlProperties();
		urls.setBackendBaseUrl(URI.create("http://localhost"));
		urls.setFrontendBaseUrl(URI.create("http://localhost:3000"));
		properties = new IdPeruProperties(new MockEnvironment(), urls);
		properties.setMode(IdPeruMode.MOCK);
		properties.setVersion(IdPeruVersion.V2);
		properties.setClientId("1234567890abcdef-client-id");
		properties.setClientSecret("test-secret");
		properties.setReferer("http://localhost");
		properties.setBaseUri(uri("/"));
		jwksBody = new AtomicReference<>(new JWKSet(key.toPublicJWK()).toString());
		tokenBody = new AtomicReference<>(tokenResponse(jwt(key, false)));
		userinfoBody = new AtomicReference<>("{\"jwt\":\"" + jwt(key, true) + "\"}");
		tokenAccept = new AtomicReference<>();
		userinfoAccept = new AtomicReference<>();
		tokenStatus = new AtomicInteger(200);
		tokenCalls = new AtomicInteger();
		userinfoTransientFailures = new AtomicInteger();
		userinfoCalls = new AtomicInteger();
		tokenDelayMillis = new AtomicLong();
		server.createContext("/service/jwks", exchange -> respond(exchange, 200, jwksBody.get()));
		server.createContext("/service/token", exchange -> {
			tokenCalls.incrementAndGet();
			tokenAccept.set(exchange.getRequestHeaders().getFirst("Accept"));
			try {
				Thread.sleep(tokenDelayMillis.get());
			}
			catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			}
			respond(exchange, tokenStatus.get(), tokenBody.get());
		});
		server.createContext("/service/userinfo", exchange -> {
			userinfoCalls.incrementAndGet();
			userinfoAccept.set(exchange.getRequestHeaders().getFirst("Accept"));
			if (userinfoTransientFailures.getAndUpdate(value -> Math.max(0, value - 1)) > 0) {
				respond(exchange, 500, "{\"error\":\"temporarily_unavailable\"}");
				return;
			}
			respond(exchange, 200, userinfoBody.get());
		});
		server.start();
	}

	@AfterEach void stop() { server.stop(0); }

	@Test
	void buildsV12AuthorizationUrlAndValidatesTokenAndUserinfoJwt() {
		IdPeruHttpClientFactory clients = testClients();
		RealIdPeruAdapter adapter = new RealIdPeruAdapter(properties, new IdPeruDniEncryptor(),
				new IdPeruJwtValidator(properties, clients), clients);
		URI authorization = adapter.authorizationUri(new CitizenIdentityProviderPort.AuthorizationContext(
				"state-test", "challenge-test", "12345678"));
		assertThat(authorization.getQuery()).contains("response_type=code", "scope=openid", "code_challenge_method=S256", "acr_values=face_mobile", "vd=");
		assertThat(authorization.getRawQuery())
				.contains("redirect_uri=http%3A%2F%2Flocalhost%2Fapi%2Fv1%2Fidperu%2Fcallback")
				.contains("vd=")
				.contains("%3D")
				.doesNotContain("redirect_uri=http://");

		CitizenIdentityProviderPort.VerifiedCitizen citizen = adapter.authenticate(
				"code-test", "session-test", "verifier-test", "12345678");
		assertThat(citizen.dni()).isEqualTo("12345678");
		assertThat(citizen.subject()).isEqualTo("subject-test");
		assertThat(tokenAccept.get()).contains(MediaType.APPLICATION_JSON_VALUE);
		assertThat(userinfoAccept.get()).contains(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test
	void buildsV1AuthorizationUrlWithoutPkceAndReadsPlainUserinfo() {
		properties.setVersion(IdPeruVersion.V1);
		tokenBody.set(new StringBuilder("{\"access_token\":\"access-test\",\"expires_in\":300,")
				.append("\"id_token\":\"unused\",\"token_type\":\"bearer\"}").toString());
		userinfoBody.set("{\"sub\":\"subject-test\",\"doc\":\"12345678\",\"first_name\":\"ANA\"}");
		IdPeruHttpClientFactory clients = testClients();
		RealIdPeruAdapter adapter = new RealIdPeruAdapter(properties, new IdPeruDniEncryptor(),
				new IdPeruJwtValidator(properties, clients), clients);

		URI authorization = adapter.authorizationUri(new CitizenIdentityProviderPort.AuthorizationContext(
				"state-test", "unused-challenge", "12345678"));

		assertThat(authorization.toString())
				.contains("/service/auth?")
				.contains("scope=openid%20profile", "acr_values=face_mobile", "vd=")
				.doesNotContain("#")
				.doesNotContain("code_challenge");
		CitizenIdentityProviderPort.VerifiedCitizen citizen = adapter.authenticate(
				"code-test", null, "unused-verifier", "12345678");
		assertThat(citizen.dni()).isEqualTo("12345678");
		assertThat(citizen.subject()).isEqualTo("subject-test");
	}

	@Test
	void rejectsAnInvalidSignatureAndRefreshesJwksForKeyRotation() throws Exception {
		IdPeruHttpClientFactory clients = testClients();
		RealIdPeruAdapter adapter = new RealIdPeruAdapter(properties, new IdPeruDniEncryptor(),
				new IdPeruJwtValidator(properties, clients), clients);
		adapter.authenticate("code-test", "session-test", "verifier-test", "12345678");

		RSAKey rotated = new RSAKeyGenerator(2048).keyID("rotated-kid").generate();
		jwksBody.set(new JWKSet(rotated.toPublicJWK()).toString());
		tokenBody.set(tokenResponse(jwt(rotated, false)));
		userinfoBody.set("{\"jwt\":\"" + jwt(rotated, true) + "\"}");
		assertThat(adapter.authenticate("code-test", "session-test", "verifier-test", "12345678").dni())
				.isEqualTo("12345678");

		RSAKey foreign = new RSAKeyGenerator(2048).keyID("rotated-kid").generate();
		tokenBody.set(tokenResponse(jwt(foreign, false)));
		assertThatThrownBy(() -> adapter.authenticate("code-test", "session-test", "verifier-test", "12345678"))
				.isInstanceOfSatisfying(IdentityIntegrationException.class,
						exception -> assertThat(exception.failure()).isEqualTo(IdentityFailure.TOKEN_INVALID));
	}

	@Test
	void mapsRejectedOrReusedAuthorizationCodeWithoutRetrying() {
		tokenStatus.set(400);
		tokenBody.set("{\"error\":\"invalid_grant\"}");
		IdPeruHttpClientFactory clients = testClients();
		RealIdPeruAdapter adapter = new RealIdPeruAdapter(properties, new IdPeruDniEncryptor(),
				new IdPeruJwtValidator(properties, clients), clients);

		assertThatThrownBy(() -> adapter.authenticate("reused-code", "session-test", "verifier-test", "12345678"))
				.isInstanceOfSatisfying(IdentityIntegrationException.class,
						exception -> assertThat(exception.failure()).isEqualTo(IdentityFailure.TOKEN_REJECTED));
	}

	@Test
	void mapsProviderTimeoutWithoutAutomaticRetry() {
		tokenDelayMillis.set(1_500);
		IdPeruHttpClientFactory clients = testClients();
		RealIdPeruAdapter adapter = new RealIdPeruAdapter(properties, new IdPeruDniEncryptor(),
				new IdPeruJwtValidator(properties, clients), clients);

		assertThatThrownBy(() -> adapter.authenticate("code-test", "session-test", "verifier-test", "12345678"))
				.isInstanceOfSatisfying(IdentityIntegrationException.class,
						exception -> assertThat(exception.failure()).isEqualTo(IdentityFailure.TIMEOUT));
	}

	@Test
	void retriesOnlyUserinfoOnceAfterATransientServerFailure() {
		properties.setVersion(IdPeruVersion.V1);
		tokenBody.set("{\"access_token\":\"access-test\",\"expires_in\":300,"
				+ "\"id_token\":\"unused\",\"token_type\":\"bearer\"}");
		userinfoBody.set("{\"sub\":\"subject-test\",\"doc\":\"12345678\",\"first_name\":\"ANA\"}");
		userinfoTransientFailures.set(1);
		IdPeruHttpClientFactory clients = testClients();
		RealIdPeruAdapter adapter = new RealIdPeruAdapter(properties, new IdPeruDniEncryptor(),
				new IdPeruJwtValidator(properties, clients), clients);

		assertThat(adapter.authenticate("code-test", null, "unused-verifier", "12345678").dni())
				.isEqualTo("12345678");
		assertThat(tokenCalls).hasValue(1);
		assertThat(userinfoCalls).hasValue(2);
	}

	private URI uri(String path) { return URI.create("http://localhost:" + server.getAddress().getPort() + path); }
	private IdPeruHttpClientFactory testClients() {
		return new IdPeruHttpClientFactory(Duration.ofSeconds(1), Duration.ofSeconds(1));
	}

	private String jwt(RSAKey signingKey, boolean userinfo) throws Exception {
		JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().issuer(properties.getIssuer())
				.subject("subject-test").expirationTime(Date.from(Instant.now().plusSeconds(120)))
				.notBeforeTime(Date.from(Instant.now().minusSeconds(1)));
		if (userinfo) claims.claim("doc", "12345678"); else claims.audience(properties.getClientId());
		SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
				.keyID(signingKey.getKeyID()).build(), claims.build());
		jwt.sign(new RSASSASigner(signingKey));
		return jwt.serialize();
	}

	private static String tokenResponse(String idToken) {
		return "{\"access_token\":\"access-test\",\"expires_in\":300,\"id_token\":\""
				+ idToken + "\",\"token_type\":\"bearer\"}";
	}

	private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws java.io.IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		try (var output = exchange.getResponseBody()) { output.write(bytes); }
	}
}
