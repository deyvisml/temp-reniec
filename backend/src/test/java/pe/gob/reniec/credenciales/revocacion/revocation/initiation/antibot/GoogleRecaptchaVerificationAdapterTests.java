package pe.gob.reniec.credenciales.revocacion.revocation.initiation.antibot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GoogleRecaptchaVerificationAdapterTests {

	private HttpServer server;
	private final AtomicReference<String> receivedForm = new AtomicReference<>();

	@AfterEach
	void stopServer() {
		if (server != null) server.stop(0);
	}

	@Test
	void acceptsSuccessForExactCaseInsensitiveAllowedHostnameAndPostsOnlyExpectedForm() throws Exception {
		var adapter = adapter(200, "{\"success\":true,\"hostname\":\"LOCALHOST\"}", 0, Set.of("localhost"));

		assertThatCode(() -> adapter.verify("ephemeral-token")).doesNotThrowAnyException();
		assertThat(receivedForm.get()).contains("secret=test-secret", "response=ephemeral-token")
				.doesNotContain("remoteip");
	}

	@Test
	void acceptsSuccessfulResponseWithoutHostname() throws Exception {
		var adapter = adapter(200, "{\"success\":true}", 0, Set.of("localhost"));
		assertThatCode(() -> adapter.verify("ephemeral-token")).doesNotThrowAnyException();
	}

	@Test
	void rejectsProviderFailureAndDisallowedHostname() throws Exception {
		assertFailure(adapter(200, "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}",
				0, Set.of("localhost")), RecaptchaFailure.REJECTED);
		stopServer();
		assertFailure(adapter(200, "{\"success\":true,\"hostname\":\"evil-localhost.example\"}",
				0, Set.of("localhost")), RecaptchaFailure.REJECTED);
	}

	@Test
	void classifiesExpiredOrReusedToken() throws Exception {
		assertFailure(adapter(200, "{\"success\":false,\"error-codes\":[\"timeout-or-duplicate\"]}",
				0, Set.of("localhost")), RecaptchaFailure.EXPIRED_OR_DUPLICATE);
	}

	@Test
	void classifiesHttpFailureAsUnavailable() throws Exception {
		assertFailure(adapter(503, "{}", 0, Set.of("localhost")), RecaptchaFailure.UNAVAILABLE);
	}

	@Test
	void classifiesTimeoutSeparately() throws Exception {
		assertFailure(adapter(200, "{\"success\":true}", 250, Set.of("localhost"), Duration.ofMillis(40)),
				RecaptchaFailure.TIMEOUT);
	}

	@Test
	void rejectsEmptyMalformedAndStructurallyInvalidBodies() throws Exception {
		assertFailure(adapter(200, "", 0, Set.of("localhost")), RecaptchaFailure.INVALID_RESPONSE);
		stopServer();
		assertFailure(adapter(200, "not-json", 0, Set.of("localhost")), RecaptchaFailure.INVALID_RESPONSE);
		stopServer();
		assertFailure(adapter(200, "{\"hostname\":\"localhost\"}", 0, Set.of("localhost")),
				RecaptchaFailure.INVALID_RESPONSE);
	}

	@Test
	void rejectsMissingAndOversizedEvidenceWithoutCallingProvider() throws Exception {
		var adapter = adapter(200, "{\"success\":true}", 0, Set.of("localhost"));
		assertFailure(adapter, " ", RecaptchaFailure.REQUIRED);
		assertFailure(adapter, "x".repeat(4097), RecaptchaFailure.REJECTED);
		assertThat(receivedForm.get()).isNull();
	}

	private GoogleRecaptchaVerificationAdapter adapter(int status, String response, long delayMillis,
			Set<String> hostnames) throws IOException {
		return adapter(status, response, delayMillis, hostnames, Duration.ofSeconds(1));
	}

	private GoogleRecaptchaVerificationAdapter adapter(int status, String response, long delayMillis,
			Set<String> hostnames, Duration timeout) throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/siteverify", exchange -> {
			try {
				receivedForm.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
				if (delayMillis > 0) Thread.sleep(delayMillis);
				byte[] body = response.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "application/json");
				exchange.sendResponseHeaders(status, body.length);
				exchange.getResponseBody().write(body);
			}
			catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			}
			finally {
				exchange.close();
			}
		});
		server.start();
		URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/siteverify");
		return new GoogleRecaptchaVerificationAdapter(
				new GoogleRecaptchaProperties("test-secret", uri, timeout, hostnames));
	}

	private void assertFailure(GoogleRecaptchaVerificationAdapter adapter, RecaptchaFailure expected) {
		assertFailure(adapter, "ephemeral-token", expected);
	}

	private void assertFailure(GoogleRecaptchaVerificationAdapter adapter, String token, RecaptchaFailure expected) {
		assertThatThrownBy(() -> adapter.verify(token))
				.isInstanceOfSatisfying(RecaptchaVerificationException.class,
						exception -> assertThat(exception.failure()).isEqualTo(expected));
	}
}
