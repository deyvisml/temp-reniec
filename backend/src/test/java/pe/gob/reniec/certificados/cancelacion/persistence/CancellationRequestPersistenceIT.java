package pe.gob.reniec.certificados.cancelacion.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.AuditEventOrigin;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationAuditEventEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationAuditEventRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationAuditEventType;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationFinalOutcome;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReasonCode;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReceiptEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationReceiptRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateCancellationRequestRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateAvailabilityCheckEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CertificateAvailabilityCheckRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CurrentAvailabilityResult;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.AvailabilityCheckResult;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.AvailabilityCheckStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.IdentityMatchResult;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.IdentityVerificationEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.IdentityVerificationRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.IdentityVerificationStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.ReceiptGenerationStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationEntity;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationRepository;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationOperationStatus;
import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.RevocationResult;
import pe.gob.reniec.certificados.cancelacion.cancellation.identity.FlowTokenService;
import pe.gob.reniec.certificados.cancelacion.cancellation.identity.IdentityFailure;
import pe.gob.reniec.certificados.cancelacion.cancellation.identity.IdentityIntegrationException;
import pe.gob.reniec.certificados.cancelacion.cancellation.identity.IdentityVerificationService;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "debug=false")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CancellationRequestPersistenceIT extends MySqlContainerSupport {

	private static final String CORRELATION = "persistence-test-correlation";

	@Autowired CertificateCancellationRequestRepository requestRepository;
	@Autowired CertificateAvailabilityCheckRepository availabilityRepository;
	@Autowired IdentityVerificationRepository identityRepository;
	@Autowired RevocationOperationRepository revocationRepository;
	@Autowired CancellationReceiptRepository receiptRepository;
	@Autowired CancellationAuditEventRepository auditRepository;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired IdentityVerificationService identityService;
	@Autowired FlowTokenService flowTokenService;

	@LocalServerPort int port;

	@BeforeEach
	void cleanTables() {
		jdbcTemplate.update("DELETE FROM cancellation_audit_event");
		jdbcTemplate.update("DELETE FROM cancellation_receipt");
		jdbcTemplate.update("DELETE FROM cancellation_request_certificate");
		jdbcTemplate.update("DELETE FROM revocation_operation");
		jdbcTemplate.update("DELETE FROM identity_verification");
		jdbcTemplate.update("DELETE FROM certificate_availability_check");
		jdbcTemplate.update("DELETE FROM certificate_cancellation_request");
	}

	@Test
	void mockIdentityFlowConsumesStateOnceAndIssuesPersistedTemporaryAuthorization() {
		CertificateCancellationRequestEntity request = new CertificateCancellationRequestEntity("00000001");
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				CancellationRequestStatus.PENDING_IDENTITY_VERIFICATION);
		request = requestRepository.saveAndFlush(request);

		FlowTokenService.IssuedFlowToken init = flowTokenService.issueIdentityInit(request.getId());
		URI authorization = identityService.start(init.value(), "identity-flow-it");
		String state = java.util.Arrays.stream(authorization.getRawQuery().split("&"))
				.filter(value -> value.startsWith("state=")).findFirst()
				.map(value -> URLDecoder.decode(value.substring(6), StandardCharsets.UTF_8)).orElseThrow();

		IdentityVerificationService.CallbackResult callback = identityService.callback(
				"mock-code", state, "mock-session", null);
		assertThat(callback.verified()).isTrue();
		assertThat(identityService.current(callback.token().value()).canContinue()).isTrue();

		IdentityVerificationEntity verification = identityRepository
				.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId()).orElseThrow();
		assertThat(verification.getVerificationStatus()).isEqualTo(IdentityVerificationStatus.VERIFIED);
		assertThat(verification.getDniMatchResult()).isEqualTo(IdentityMatchResult.MATCH);
		assertThat(verification.getPkceVerifierProtected()).isNull();
		assertThat(verification.getAuthorizationJtiHash()).hasSize(64);
		assertThatThrownBy(() -> identityService.callback("mock-code", state, "mock-session", null))
				.isInstanceOf(IdentityIntegrationException.class);

		CertificateCancellationRequestEntity completed = requestRepository.findById(request.getId()).orElseThrow();
		completed.transitionTo(CancellationRequestStatus.COMPLETED, null);
		requestRepository.saveAndFlush(completed);
		assertThat(identityService.current(callback.token().value()).canContinue()).isFalse();
	}

	@Test
	void rejectsASecondIdentityStartWhileTheCurrentAttemptRemainsValid() {
		CertificateCancellationRequestEntity request = new CertificateCancellationRequestEntity("00000001");
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				CancellationRequestStatus.PENDING_IDENTITY_VERIFICATION);
		request = requestRepository.saveAndFlush(request);

		FlowTokenService.IssuedFlowToken init = flowTokenService.issueIdentityInit(request.getId());
		identityService.start(init.value(), "first-identity-start");

		assertThatThrownBy(() -> identityService.start(init.value(), "duplicate-identity-start"))
				.isInstanceOfSatisfying(IdentityIntegrationException.class,
						exception -> assertThat(exception.failure()).isEqualTo(IdentityFailure.IN_PROGRESS));
		assertThat(identityRepository.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId()))
				.get().extracting(IdentityVerificationEntity::getAttemptNumber).isEqualTo(1);
	}

	@Test
	void malformedOptionalCallbackValuesFinishTheAttemptWithoutOverflowingPersistence() {
		CertificateCancellationRequestEntity request = new CertificateCancellationRequestEntity("00000001");
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				CancellationRequestStatus.PENDING_IDENTITY_VERIFICATION);
		request = requestRepository.saveAndFlush(request);

		FlowTokenService.IssuedFlowToken init = flowTokenService.issueIdentityInit(request.getId());
		URI authorization = identityService.start(init.value(), "callback-boundary-it");
		String state = java.util.Arrays.stream(authorization.getRawQuery().split("&"))
				.filter(value -> value.startsWith("state="))
				.map(value -> URLDecoder.decode(value.substring(6), StandardCharsets.UTF_8))
				.findFirst().orElseThrow();

		IdentityVerificationService.CallbackResult result = identityService.callback(
				"mock-code", state, "x".repeat(257), null);

		assertThat(result.verified()).isFalse();
		assertThat(result.status()).isEqualTo("ERROR");
		IdentityVerificationEntity verification = identityRepository
				.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId()).orElseThrow();
		assertThat(verification.getVerificationStatus()).isEqualTo(IdentityVerificationStatus.ERROR);
		assertThat(verification.getErrorOrCancellationCode()).isEqualTo("INVALID_CALLBACK");
		assertThat(verification.getProviderSessionState()).isNull();
	}

	@Test
	void httpMockFlowRotatesTheCookieAndEnablesCertificateSelection() throws Exception {
		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
		HttpResponse<String> initiation = client.send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/api/v1/cancellation-requests"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(
						"{\"dni\":\"00000001\",\"recaptchaToken\":\"test-recaptcha-valid\"}"))
				.build(), HttpResponse.BodyHandlers.ofString());
		String initiationCookie = cookiePair(initiation);

		HttpResponse<String> start = client.send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/api/v1/identity-verifications"))
				.header("Cookie", initiationCookie)
				.POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
		java.util.regex.Matcher authorization = java.util.regex.Pattern
				.compile("\\\"authorizationUrl\\\":\\\"([^\\\"]+)\\\"").matcher(start.body());
		assertThat(authorization.find()).isTrue();
		URI authorizationUri = URI.create(authorization.group(1));
		String state = java.util.Arrays.stream(authorizationUri.getRawQuery().split("&"))
				.filter(value -> value.startsWith("state="))
				.map(value -> URLDecoder.decode(value.substring(6), StandardCharsets.UTF_8))
				.findFirst().orElseThrow();

		HttpResponse<String> callback = client.send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/api/v1/idperu/callback?code=mock-code&state="
						+ java.net.URLEncoder.encode(state, StandardCharsets.UTF_8)
						+ "&session_state=mock-session"))
				.GET().build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(callback.statusCode()).withFailMessage("Callback body: %s", callback.body()).isEqualTo(303);
		assertThat(callback.headers().firstValue("Location")).hasValue("http://localhost:3000/cancelacion");
		String authorizationCookie = cookiePair(callback);

		HttpResponse<String> current = client.send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/api/v1/identity-verifications/current"))
				.header("Cookie", authorizationCookie).GET().build(), HttpResponse.BodyHandlers.ofString());

		assertThat(initiation.statusCode()).isEqualTo(200);
		assertThat(start.statusCode()).isEqualTo(200);
		assertThat(current.statusCode()).isEqualTo(200);
		assertThat(current.body()).contains("\"status\":\"VERIFIED\"", "\"canContinue\":true",
				"\"nextStep\":\"CERTIFICATE_SELECTION\"")
				.doesNotContain("00000001", "mock-code", state);
		assertThat(initiationCookie).isNotEqualTo(authorizationCookie);
	}

	@Test
	void invalidBrowserCallbackRedirectsWithoutExposingTheApiErrorDocument() throws Exception {
		HttpResponse<String> callback = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
				.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
						+ "/api/v1/idperu/callback?code=provider-code&state=unknown-state"))
						.GET().build(), HttpResponse.BodyHandlers.ofString());

		assertThat(callback.statusCode()).isEqualTo(HttpStatus.SEE_OTHER.value());
		assertThat(callback.headers().firstValue("Location")).hasValue("http://localhost:3000/cancelacion");
		assertThat(callback.headers().allValues("Set-Cookie")).anySatisfy(cookie -> assertThat(cookie)
				.contains("idperu_callback_outcome=ERROR", "HttpOnly", "SameSite=Lax")
				.doesNotContain("provider-code", "unknown-state"));
		assertThat(callback.body()).isEmpty();
	}

	@Test
	void postCallbackRemainsCompatibleAndRedirectsWithoutTechnicalBody() throws Exception {
		String form = "code=provider-code&state=unknown-state&session_state=provider-session";
		HttpResponse<String> callback = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
				.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/idperu/callback"))
						.header("Content-Type", "application/x-www-form-urlencoded")
						.POST(HttpRequest.BodyPublishers.ofString(form)).build(),
						HttpResponse.BodyHandlers.ofString());

		assertThat(callback.statusCode()).isEqualTo(HttpStatus.SEE_OTHER.value());
		assertThat(callback.headers().firstValue("Location")).hasValue("http://localhost:3000/cancelacion");
		assertThat(callback.headers().allValues("Set-Cookie")).anySatisfy(cookie -> assertThat(cookie)
				.contains("idperu_callback_outcome=ERROR", "HttpOnly", "SameSite=Lax")
				.doesNotContain("provider-code", "unknown-state", "provider-session"));
		assertThat(callback.body()).isEmpty();
	}

	@Test
	void cleanDatabaseRunsFlywayCreatesTheSevenDomainTablesAndReportsSafeHealth() throws Exception {
		List<String> tables = jdbcTemplate.queryForList("""
				SELECT table_name FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				ORDER BY table_name
				""", String.class);
		List<String> obsoleteColumns = jdbcTemplate.queryForList("""
				SELECT column_name FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				AND column_name IN (
				 'dni_lookup_hash', 'dni_ciphertext', 'dni_key_version', 'dni_last_four',
				 'other_reason_ciphertext', 'other_reason_key_version', 'lifecycle_status',
				 'active_dni_guard', 'verified_identity_hash', 'session_reference_hash',
				 'token_family_id', 'client_reference_hash', 'open_request_guard',
				 'next_status_check_at', 'document_hash', 'template_version',
				 'technical_code', 'technical_detail', 'public_reference', 'consent_version',
				 'recoverable_until', 'expires_at', 'session_reference')
				""", String.class);
		Integer migrationCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE", Integer.class);
		List<String> tablesWithoutComments = jdbcTemplate.queryForList("""
				SELECT table_name FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				AND TRIM(COALESCE(table_comment, '')) = ''
				ORDER BY table_name
				""", String.class);
		List<String> columnsWithoutComments = jdbcTemplate.queryForList("""
				SELECT CONCAT(table_name, '.', column_name) FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				AND TRIM(COALESCE(column_comment, '')) = ''
				ORDER BY table_name, ordinal_position
				""", String.class);
		Integer documentedColumnCount = jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				AND TRIM(COALESCE(column_comment, '')) <> ''
				""", Integer.class);
		HttpResponse<String> health = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health")).GET().build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(tables).containsExactly(
				"cancellation_audit_event", "cancellation_receipt",
				"cancellation_request_certificate", "certificate_availability_check",
				"certificate_cancellation_request", "identity_verification", "revocation_operation");
		assertThat(obsoleteColumns).isEmpty();
		assertThat(migrationCount).isEqualTo(6);
		assertThat(tablesWithoutComments).isEmpty();
		assertThat(columnsWithoutComments).isEmpty();
		assertThat(documentedColumnCount).isEqualTo(92);
		assertThat(health.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(health.body()).contains("\"status\":\"UP\"")
				.doesNotContain("jdbc", "mysql", "username", "password", "sql", "dni");
	}

	@Test
	void exposesExactSpanishCommentsAndPreservesRepresentativeColumnDefinitions() {
		assertThat(tableComment("certificate_cancellation_request"))
				.isEqualTo("Solicitud ciudadana que concentra el estado y resultado actual del trámite de cancelación");
		assertThat(columnComment("certificate_cancellation_request", "dni"))
				.isEqualTo("Número de DNI asociado a la solicitud ciudadana");
		assertThat(columnComment("cancellation_request_certificate", "selected"))
				.isEqualTo("Indica si el ciudadano seleccionó el certificado para cancelarlo");
		assertThat(columnComment("revocation_operation", "idempotency_key"))
				.isEqualTo("Clave única que evita ejecutar dos veces la misma operación técnica");
		assertThat(columnComment("revocation_operation", "normalized_result"))
				.isEqualTo("Resultado general normalizado de la operación técnica");
		assertThat(columnComment("cancellation_receipt", "receipt_code"))
				.isEqualTo("Código único asignado a la constancia");
		assertThat(columnComment("cancellation_audit_event", "event_type"))
				.isEqualTo("Tipo de evento relevante registrado por el backend");

		Map<String, Object> requestId = columnDefinition("certificate_cancellation_request", "id");
		assertThat(requestId).containsEntry("column_type", "bigint unsigned")
				.containsEntry("is_nullable", "NO")
				.containsEntry("extra", "auto_increment");

		Map<String, Object> dni = columnDefinition("certificate_cancellation_request", "dni");
		assertThat(dni).containsEntry("column_type", "char(8)")
				.containsEntry("is_nullable", "NO")
				.containsEntry("character_set_name", "ascii")
				.containsEntry("collation_name", "ascii_bin");

		Map<String, Object> selected = columnDefinition("cancellation_request_certificate", "selected");
		assertThat(selected).containsEntry("column_type", "tinyint(1)")
				.containsEntry("is_nullable", "NO")
				.containsEntry("column_default", "0");

		Map<String, Object> version = columnDefinition("cancellation_request_certificate", "version");
		assertThat(version).containsEntry("column_type", "bigint unsigned")
				.containsEntry("is_nullable", "NO")
				.containsEntry("column_default", "0");
	}

	@Test
	void serializesConcurrentAvailabilityAndStartsANewRequestForALaterEntry() throws Exception {
		String body = "{\"dni\":\"00000001\",\"recaptchaToken\":\"test-recaptcha-valid\"}";
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> { start.await(); return postCancellation(body); });
			var second = executor.submit(() -> { start.await(); return postCancellation(body); });
			start.countDown();
			HttpResponse<String> firstResponse = first.get();
			HttpResponse<String> secondResponse = second.get();
			assertThat(List.of(firstResponse.statusCode(), secondResponse.statusCode()))
					.allMatch(status -> status == 200 || status == 409);
			assertThat(firstResponse.body() + secondResponse.body())
					.contains("AVAILABLE", "IDENTITY_VERIFICATION", "******01")
					.doesNotContain("00000001", "\"id\"");
		}
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM certificate_cancellation_request WHERE dni='00000001'", Integer.class))
				.isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM certificate_availability_check", Integer.class)).isEqualTo(1);
		assertThat(certificateCount()).isZero();

		Long firstRequestId = jdbcTemplate.queryForObject(
				"SELECT id FROM certificate_cancellation_request WHERE dni='00000001'", Long.class);
		HttpResponse<String> fresh = postCancellation(body);
		assertThat(fresh.statusCode()).isEqualTo(200);
		assertThat(fresh.body()).contains("\"requestId\":")
				.doesNotContain("reused", "requestReference", "00000001");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM certificate_cancellation_request WHERE dni='00000001'", Integer.class))
				.isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM certificate_availability_check", Integer.class)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT request_status FROM certificate_cancellation_request WHERE id=?",
				String.class, firstRequestId)).isEqualTo("ABANDONED");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM certificate_availability_check WHERE attempt_number=1", Integer.class))
				.isEqualTo(2);
		assertThat(certificateCount()).isZero();
	}

	@Test
	void blocksProtectedRequestWithoutDisclosingHistoricalData() throws Exception {
		CertificateCancellationRequestEntity protectedRequest = saveRequest("00000001");
		protectedRequest.transitionTo(CancellationRequestStatus.REVOCATION_IN_PROGRESS, null);
		requestRepository.saveAndFlush(protectedRequest);

		HttpResponse<String> response = postCancellation("{\"dni\":\"00000001\",\"recaptchaToken\":\"test-recaptcha-valid\"}");

		assertThat(response.statusCode()).isEqualTo(409);
		assertThat(response.body()).contains("CANCELLATION_REQUEST_IN_PROGRESS", "correlationId")
				.doesNotContain("00000001", "requestId", "constancia", "certificate");
		assertThat(requestRepository.count()).isEqualTo(1);
		assertThat(availabilityRepository.count()).isZero();
	}

	@Test
	void rejectsInvalidDniWithoutPersistenceAndReturnsCorrelation() throws Exception {
		HttpResponse<String> response = postCancellation("{\"dni\":\"1234\",\"recaptchaToken\":\"test-recaptcha-valid\"}");
		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("VALIDATION_ERROR", "correlationId")
				.doesNotContain("1234");
		assertThat(requestRepository.count()).isZero();
		assertThat(availabilityRepository.count()).isZero();
	}

	@Test
	void rejectedCaptchaCreatesNeitherRequestNorAvailabilityAttempt() throws Exception {
		HttpResponse<String> response = postCancellation(
				"{\"dni\":\"00000001\",\"recaptchaToken\":\"test-recaptcha-invalid\"}");

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("RECAPTCHA_REJECTED", "correlationId")
				.doesNotContain("test-recaptcha-invalid", "00000001");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM certificate_cancellation_request", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM certificate_availability_check", Integer.class)).isZero();
	}

	@Test
	void persistsEveryDeterministicAlternativeOutcomeWithControlledHttpSemantics() throws Exception {
		HttpResponse<String> notAvailable = postCancellation("{\"dni\":\"00000002\",\"recaptchaToken\":\"test-recaptcha-valid\"}");
		HttpResponse<String> unavailable = postCancellation("{\"dni\":\"00000003\",\"recaptchaToken\":\"test-recaptcha-valid\"}");
		HttpResponse<String> inconclusive = postCancellation("{\"dni\":\"00000004\",\"recaptchaToken\":\"test-recaptcha-valid\"}");
		HttpResponse<String> technical = postCancellation("{\"dni\":\"00000005\",\"recaptchaToken\":\"test-recaptcha-valid\"}");
		HttpResponse<String> timeout = postCancellation("{\"dni\":\"00000006\",\"recaptchaToken\":\"test-recaptcha-valid\"}");

		assertThat(notAvailable.statusCode()).isEqualTo(200);
		assertThat(notAvailable.body()).contains("NOT_AVAILABLE").doesNotContain("00000002");
		assertThat(unavailable.statusCode()).isEqualTo(503);
		assertThat(unavailable.body()).contains("AVAILABILITY_UNAVAILABLE").doesNotContain("00000003");
		assertThat(inconclusive.statusCode()).isEqualTo(200);
		assertThat(inconclusive.body()).contains("INCONCLUSIVE").doesNotContain("00000004");
		assertThat(technical.statusCode()).isEqualTo(502);
		assertThat(technical.body()).contains("AVAILABILITY_PROVIDER_ERROR").doesNotContain("00000005");
		assertThat(timeout.statusCode()).isEqualTo(504);
		assertThat(timeout.body()).contains("AVAILABILITY_TIMEOUT").doesNotContain("00000006");
		assertThat(requestRepository.count()).isEqualTo(5);
		assertThat(availabilityRepository.count()).isEqualTo(5);
		assertThat(certificateCount()).isZero();
		assertThat(notAvailable.body() + unavailable.body() + inconclusive.body() + technical.body() + timeout.body())
				.doesNotContain("certificateUuid", "orderNumber", "emissionCreatedAt", "certificateCount");
	}

	@Test
	void storesReadableRequestReasonConfirmationAndTerminalHistoryWithoutExpiration() {
		CertificateCancellationRequestEntity request = saveRequest("12345678");
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				CancellationRequestStatus.PENDING_IDENTITY_VERIFICATION);
		request.registerReason(CancellationReasonCode.OTHER, "Cambio de dispositivo personal");
		request.confirm(Instant.now());
		requestRepository.saveAndFlush(request);

		assertThat(requestRepository.findById(request.getId())).get().satisfies(found -> {
			assertThat(found.getDni()).isEqualTo("12345678");
			assertThat(found.getRequestStatus()).isEqualTo(CancellationRequestStatus.CONFIRMED);
			assertThat(found.getAvailabilityResult()).isEqualTo(CurrentAvailabilityResult.AVAILABLE);
			assertThat(found.getReasonCode()).isEqualTo(CancellationReasonCode.OTHER);
			assertThat(found.getOtherReason()).isEqualTo("Cambio de dispositivo personal");
			assertThat(found.getConfirmedAt()).isNotNull();
		});
		assertThatThrownBy(() -> request.registerReason(CancellationReasonCode.LOSS, null))
				.isInstanceOf(IllegalStateException.class);
		assertThat(requestRepository.findFirstByDniAndRequestStatusInOrderByCreatedAtDesc(
				"12345678", Set.of(CancellationRequestStatus.CONFIRMED))).get()
				.extracting(CertificateCancellationRequestEntity::getId).isEqualTo(request.getId());
		assertThat(requestRepository.findFirstByDniOrderByCreatedAtDesc("12345678")).get()
				.extracting(CertificateCancellationRequestEntity::getId).isEqualTo(request.getId());

		CertificateCancellationRequestEntity historical = saveRequest("12345678");
		historical.transitionTo(CancellationRequestStatus.ABANDONED, CancellationFinalOutcome.ABANDONED);
		requestRepository.saveAndFlush(historical);
		assertThat(requestRepository.count()).isEqualTo(2);
		assertThatThrownBy(() -> new CertificateCancellationRequestEntity("1234ABCD"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void recordsRepeatableAvailabilityAndIdentityAttemptsAndFindsLatestValid() {
		CertificateCancellationRequestEntity request = saveRequest("23456789");
		Instant now = Instant.now();
		CertificateAvailabilityCheckEntity eligibility1 = new CertificateAvailabilityCheckEntity(
				request, 1, AvailabilityCheckStatus.SUBMITTED, now, CORRELATION);
		eligibility1.complete(AvailabilityCheckResult.INCONCLUSIVE, now.plusSeconds(1), "eligibility-ref-1");
		availabilityRepository.saveAndFlush(eligibility1);
		CertificateAvailabilityCheckEntity eligibility2 = new CertificateAvailabilityCheckEntity(
				request, 2, AvailabilityCheckStatus.SUBMITTED, now.plusSeconds(2), CORRELATION);
		eligibility2.complete(AvailabilityCheckResult.AVAILABLE, now.plusSeconds(3), "eligibility-ref-2");
		availabilityRepository.saveAndFlush(eligibility2);

		IdentityVerificationEntity identity1 = new IdentityVerificationEntity(request, 1, "ID_PERU", now, CORRELATION);
		identity1.finish(IdentityVerificationStatus.REJECTED, IdentityMatchResult.INCONCLUSIVE,
				now.plusSeconds(1), "identity-ref-1", "REJECTED_BY_PROVIDER");
		identityRepository.saveAndFlush(identity1);
		IdentityVerificationEntity identity2 = new IdentityVerificationEntity(
				request, 2, "ID_PERU", now.plusSeconds(2), CORRELATION);
		identity2.finish(IdentityVerificationStatus.VERIFIED, IdentityMatchResult.MATCH,
				now.plusSeconds(3), "identity-ref-2", null);
		identityRepository.saveAndFlush(identity2);

		assertThat(availabilityRepository.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId())).get()
				.extracting(CertificateAvailabilityCheckEntity::getId).isEqualTo(eligibility2.getId());
		assertThat(identityRepository.findFirstByRequest_IdAndVerificationStatusOrderByAttemptNumberDesc(
				request.getId(), IdentityVerificationStatus.VERIFIED)).get()
				.extracting(IdentityVerificationEntity::getId).isEqualTo(identity2.getId());
		assertThatThrownBy(() -> availabilityRepository.saveAndFlush(new CertificateAvailabilityCheckEntity(
				request, 2, AvailabilityCheckStatus.CREATED, now.plusSeconds(4), CORRELATION)))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> identityRepository.saveAndFlush(new IdentityVerificationEntity(
				request, 2, "ID_PERU", now.plusSeconds(4), CORRELATION)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void keepsUncertainRevocationOnTheSameOperationAndEnforcesIdempotencyAndAttemptUniqueness() {
		CertificateCancellationRequestEntity request = saveRequest("45678901");
		String key = "revoke-request-001";
		Instant now = Instant.now();
		RevocationOperationEntity uncertain = new RevocationOperationEntity(request, key, 1, now, CORRELATION);
		uncertain.markSubmitted(now.plusSeconds(1), "revocation-ref-1");
		uncertain.complete(RevocationOperationStatus.OUTCOME_UNKNOWN, RevocationResult.OUTCOME_UNKNOWN,
				now.plusSeconds(2), null, "PROVIDER_TIMEOUT");
		revocationRepository.saveAndFlush(uncertain);

		assertThat(revocationRepository.findByIdempotencyKey(key)).get()
				.extracting(RevocationOperationEntity::getId).isEqualTo(uncertain.getId());
		assertThat(revocationRepository.findFirstByRequest_IdAndOperationStatusInOrderByAttemptNumberDesc(
				request.getId(), Set.of(RevocationOperationStatus.OUTCOME_UNKNOWN))).get()
				.extracting(RevocationOperationEntity::getId).isEqualTo(uncertain.getId());
		assertThatThrownBy(() -> revocationRepository.saveAndFlush(
				new RevocationOperationEntity(request, "revoke-request-002", 1, now.plusSeconds(3), CORRELATION)))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> revocationRepository.saveAndFlush(
				new RevocationOperationEntity(request, key, 2, now.plusSeconds(4), CORRELATION)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void associatesReceiptsWithSuccessfulRevocationAndKeepsReceiptFailureIndependent() {
		CertificateCancellationRequestEntity request = saveRequest("56789012");
		Instant now = Instant.now();
		RevocationOperationEntity operation = new RevocationOperationEntity(
				request, "revoke-receipt-001", 1, now, CORRELATION);
		operation.complete(RevocationOperationStatus.SUCCEEDED, RevocationResult.SUCCEEDED,
				now.plusSeconds(1), now.plusSeconds(1), null);
		revocationRepository.saveAndFlush(operation);
		request.transitionTo(CancellationRequestStatus.COMPLETED, CancellationFinalOutcome.REVOCATION_SUCCEEDED);
		requestRepository.saveAndFlush(request);

		CancellationReceiptEntity available = new CancellationReceiptEntity(request, operation, "CD-TEST-0001");
		available.markAvailable("documents/receipt-0001", now.plusSeconds(2), now.plusSeconds(3));
		receiptRepository.saveAndFlush(available);
		CancellationReceiptEntity failed = new CancellationReceiptEntity(request, operation, "CD-TEST-0002");
		failed.markFailed("RENDER_FAILURE");
		receiptRepository.saveAndFlush(failed);

		assertThat(receiptRepository.findFirstByRequest_IdAndGenerationStatusOrderByAvailableAtDesc(
				request.getId(), ReceiptGenerationStatus.AVAILABLE)).get()
				.extracting(CancellationReceiptEntity::getId).isEqualTo(available.getId());
		assertThat(requestRepository.findById(request.getId())).get()
				.extracting(CertificateCancellationRequestEntity::getFinalOutcome)
				.isEqualTo(CancellationFinalOutcome.REVOCATION_SUCCEEDED);
		assertThatThrownBy(() -> receiptRepository.saveAndFlush(
				new CancellationReceiptEntity(request, operation, "CD-TEST-0001")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void appendsOrderedMinimalAuditHistoryAndRejectsOrphanRows() {
		CertificateCancellationRequestEntity request = saveRequest("67890123");
		Instant now = Instant.now();
		CancellationAuditEventEntity started = auditRepository.save(new CancellationAuditEventEntity(
				request, CancellationAuditEventType.REQUEST_STARTED, null, CancellationRequestStatus.STARTED,
				"CREATED", CORRELATION, AuditEventOrigin.CITIZEN, now));
		CancellationAuditEventEntity checked = auditRepository.save(new CancellationAuditEventEntity(
				request, CancellationAuditEventType.ELIGIBILITY_CHECKED, CancellationRequestStatus.STARTED,
				CancellationRequestStatus.PENDING_IDENTITY_VERIFICATION, "AVAILABLE", CORRELATION,
				AuditEventOrigin.EXTERNAL_PROVIDER, now.plusSeconds(1)));

		assertThat(auditRepository.findByRequest_IdOrderByOccurredAtAscIdAsc(request.getId()))
				.extracting(CancellationAuditEventEntity::getId).containsExactly(started.getId(), checked.getId());
		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO cancellation_audit_event
				(request_id, event_type, correlation_id, event_origin, occurred_at)
				VALUES (?, 'REQUEST_STARTED', ?, 'SYSTEM', CURRENT_TIMESTAMP(6))
				""", Long.MAX_VALUE, CORRELATION)).isInstanceOf(DataIntegrityViolationException.class);
	}

	private CertificateCancellationRequestEntity saveRequest(String dni) {
		return requestRepository.saveAndFlush(new CertificateCancellationRequestEntity(dni));
	}

	private String tableComment(String tableName) {
		return jdbcTemplate.queryForObject("""
				SELECT table_comment FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name = ?
				""", String.class, tableName);
	}

	private String columnComment(String tableName, String columnName) {
		return jdbcTemplate.queryForObject("""
				SELECT column_comment FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
				""", String.class, tableName, columnName);
	}

	private Map<String, Object> columnDefinition(String tableName, String columnName) {
		return jdbcTemplate.queryForMap("""
				SELECT column_type, is_nullable, column_default, extra, character_set_name, collation_name
				FROM information_schema.columns
				WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
				""", tableName, columnName);
	}

	private HttpResponse<String> postCancellation(String body) throws Exception {
		return HttpClient.newHttpClient().send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/api/v1/cancellation-requests"))
				.header("Content-Type", "application/json")
				.header("X-Correlation-ID", "eligibility-it")
				.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
	}

	private Integer certificateCount() {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cancellation_request_certificate", Integer.class);
	}

	private static String cookiePair(HttpResponse<?> response) {
		return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
	}
}
