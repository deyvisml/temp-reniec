package pe.gob.reniec.credenciales.revocacion.persistence;

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

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.AuditEventOrigin;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationAuditEventEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationAuditEventRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationAuditEventType;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationFinalOutcome;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationFlowSessionRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReasonCode;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestDigitalCredentialEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestDigitalCredentialRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReceiptEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationReceiptRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialRevocationRequestRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialAvailabilityCheckEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialAvailabilityCheckRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.CurrentAvailabilityResult;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialAvailabilityStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.AvailabilityCheckResult;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.AvailabilityCheckStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityMatchResult;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.IdentityVerificationStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.ReceiptGenerationStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationRepository;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationOperationStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationResult;
import pe.gob.reniec.credenciales.revocacion.revocation.session.FlowSessionService;
import pe.gob.reniec.credenciales.revocacion.revocation.session.FlowSessionException;
import pe.gob.reniec.credenciales.revocacion.revocation.identity.IdentityFailure;
import pe.gob.reniec.credenciales.revocacion.revocation.identity.IdentityIntegrationException;
import pe.gob.reniec.credenciales.revocacion.revocation.identity.IdentityPersistenceCoordinator;
import pe.gob.reniec.credenciales.revocacion.revocation.identity.IdentitySecurityArtifacts;
import pe.gob.reniec.credenciales.revocacion.revocation.identity.IdentityVerificationService;
import pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials.DigitalCredentialListingService;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "debug=false")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RevocationRequestPersistenceIT extends MySqlContainerSupport {

	private static final String CORRELATION = "persistence-test-correlation";

	@Autowired DigitalCredentialRevocationRequestRepository requestRepository;
	@Autowired DigitalCredentialAvailabilityCheckRepository availabilityRepository;
	@Autowired IdentityVerificationRepository identityRepository;
	@Autowired RevocationOperationRepository revocationRepository;
	@Autowired RevocationReceiptRepository receiptRepository;
	@Autowired RevocationAuditEventRepository auditRepository;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired IdentityVerificationService identityService;
	@Autowired IdentityPersistenceCoordinator identityPersistence;
	@Autowired IdentitySecurityArtifacts identitySecurity;
	@Autowired FlowSessionService flowSessionService;
	@Autowired DigitalCredentialListingService digitalCredentialListingService;
	@Autowired RevocationFlowSessionRepository flowSessionRepository;
	@Autowired RevocationRequestDigitalCredentialRepository digitalCredentialRepository;

	@LocalServerPort int port;

	@BeforeEach
	void cleanTables() {
		jdbcTemplate.update("DELETE FROM revocation_audit_event");
		jdbcTemplate.update("DELETE FROM revocation_receipt");
		jdbcTemplate.update("DELETE FROM revocation_request_digital_credential");
		jdbcTemplate.update("DELETE FROM revocation_operation");
		jdbcTemplate.update("DELETE FROM identity_verification");
		jdbcTemplate.update("DELETE FROM revocation_flow_session");
		jdbcTemplate.update("DELETE FROM digital_credential_availability_check");
		jdbcTemplate.update("DELETE FROM digital_credential_revocation_request");
	}

	@Test
	void confirmsAndExecutesExactlyOneRevocationThroughTheProtectedHttpApi() throws Exception {
		DigitalCredentialRevocationRequestEntity request = new DigitalCredentialRevocationRequestEntity("73905791");
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION);
		request = requestRepository.saveAndFlush(request);
		flowSessionService.establish(request.getId());

		IdentityVerificationEntity verification = new IdentityVerificationEntity(
				request, 1, "ID_PERU", Instant.now().minusSeconds(30), "confirmation-http-it");
		verification.finish(IdentityVerificationStatus.VERIFIED, IdentityMatchResult.MATCH,
				Instant.now().minusSeconds(20), "identity-http-it", null, null, null, "ANA");
		identityRepository.saveAndFlush(verification);

		request.transitionTo(RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE, null);
		requestRepository.saveAndFlush(request);
		RevocationRequestDigitalCredentialEntity digitalCredential = new RevocationRequestDigitalCredentialEntity(
				request, 31, "DniPeruanoCredential", Instant.parse("2026-07-15T15:24:00Z"),
				"11111111-1111-4111-8111-111111111111",
				DigitalCredentialAvailabilityStatus.AVAILABLE, null, 0, Instant.now().minusSeconds(10));
		digitalCredentialRepository.saveAndFlush(digitalCredential);
		String access = flowSessionService.markIdentityVerified(request.getId()).value();

		String draft = """
				{"digitalCredentialUuid":"11111111-1111-4111-8111-111111111111","statusListIndex":31,"reasonCode":"THEFT"}
				""";
		HttpResponse<String> review = currentRequest("POST", "/review", access, draft);
		assertThat(review.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(review.body()).contains("******91", "\"statusListIndex\":31")
				.doesNotContain("73905791", "11111111-1111-4111-8111-111111111111");
		assertThat(digitalCredentialRepository.countByRequest_IdAndSelectedTrue(request.getId())).isZero();
		assertThat(requestRepository.findById(request.getId()).orElseThrow().getReasonCode()).isNull();

		String confirmation = """
				{"digitalCredentialUuid":"11111111-1111-4111-8111-111111111111","statusListIndex":31,"reasonCode":"THEFT",
				"consentAccepted":true,"consentVersion":"REVOCACION_CREDENCIALES_DIGITALES_V1"}
				""";
		HttpResponse<String> first;
		HttpResponse<String> repeated;
		try (var executor = Executors.newFixedThreadPool(2)) {
			var firstCall = executor.submit(() -> currentRequest(
					"POST", "/confirmation", access, confirmation));
			var repeatedCall = executor.submit(() -> currentRequest(
					"POST", "/confirmation", access, confirmation));
			first = firstCall.get();
			repeated = repeatedCall.get();
		}
		assertThat(first.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(repeated.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(first.body() + repeated.body()).contains("\"state\":\"SUCCEEDED\"",
				"\"requestStatus\":\"RECEIPT_AVAILABLE\"", "\"downloadAvailable\":true")
				.doesNotContain("11111111-1111-4111-8111-111111111111");
		assertThat(jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM revocation_audit_event
				WHERE request_id = ? AND event_type = 'CONSENT_CONFIRMED'
				""", Integer.class, request.getId())).isEqualTo(1);
		assertThat(revocationRepository.count()).isEqualTo(1);
		assertThat(revocationRepository.findAll()).singleElement()
				.extracting(RevocationOperationEntity::getExternalReference)
				.isEqualTo("mock-revocation-request-" + request.getId());
		assertThat(receiptRepository.count()).isEqualTo(1);
		assertThat(digitalCredentialRepository.countByRequest_IdAndSelectedTrue(request.getId())).isEqualTo(1);
		assertThat(requestRepository.findById(request.getId()).orElseThrow().getRequestStatus())
				.isEqualTo(RevocationRequestStatus.RECEIPT_AVAILABLE);
	}

	@Test
	void mockIdentityFlowConsumesStateOnceAndUsesTheSharedFlowSession() {
		DigitalCredentialRevocationRequestEntity request = new DigitalCredentialRevocationRequestEntity("00000001");
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION);
		request = requestRepository.saveAndFlush(request);

		FlowSessionService.Tokens init = flowSessionService.establish(request.getId());
		URI authorization = identityService.start(init.access().value(), "identity-flow-it");
		String state = java.util.Arrays.stream(authorization.getRawQuery().split("&"))
				.filter(value -> value.startsWith("state=")).findFirst()
				.map(value -> URLDecoder.decode(value.substring(6), StandardCharsets.UTF_8)).orElseThrow();

		IdentityVerificationService.CallbackResult callback = identityService.callback(
				"mock-code", state, "mock-session", null);
		assertThat(callback.verified()).isTrue();
		assertThat(identityService.current(callback.accessToken().value()).canContinue()).isTrue();

		IdentityVerificationEntity verification = identityRepository
				.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId()).orElseThrow();
		assertThat(verification.getVerificationStatus()).isEqualTo(IdentityVerificationStatus.VERIFIED);
		assertThat(verification.getDniMatchResult()).isEqualTo(IdentityMatchResult.MATCH);
		assertThat(verification.getVerifiedFirstName()).isEqualTo("PRUEBA");
		assertThat(verification.getPkceVerifierProtected()).isNull();
		assertThatThrownBy(() -> identityService.start(callback.accessToken().value(), "identity-already-verified"))
				.isInstanceOfSatisfying(IdentityIntegrationException.class,
						exception -> assertThat(exception.failure()).isEqualTo(IdentityFailure.UNAUTHORIZED));
		assertThat(identityRepository.findAll()).singleElement()
				.extracting(IdentityVerificationEntity::getAttemptNumber).isEqualTo(1);
		assertThatThrownBy(() -> identityService.callback("mock-code", state, "mock-session", null))
				.isInstanceOf(IdentityIntegrationException.class);

		DigitalCredentialRevocationRequestEntity completed = requestRepository.findById(request.getId()).orElseThrow();
		completed.transitionTo(RevocationRequestStatus.COMPLETED, null);
		requestRepository.saveAndFlush(completed);
		assertThat(identityService.current(callback.accessToken().value()).canContinue()).isFalse();
	}

	@Test
	void rotatesRefreshOnceRejectsConcurrentReuseAndLogoutAbandonsTheActiveOperation() {
		DigitalCredentialRevocationRequestEntity request = new DigitalCredentialRevocationRequestEntity("00000001");
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION);
		request = requestRepository.saveAndFlush(request);

		FlowSessionService.Tokens initial = flowSessionService.establish(request.getId());
		assertThat(flowSessionService.current(initial.access().value()).requestId()).isEqualTo(request.getId());
		assertThat(java.time.Duration.between(Instant.now(), initial.access().expiresAt()).toMinutes())
				.isBetween(14L, 15L);
		assertThat(java.time.Duration.between(Instant.now(), initial.refresh().expiresAt()).toHours())
				.isBetween(71L, 72L);

		FlowSessionService.Tokens rotated = flowSessionService.refresh(initial.refresh().value());
		assertThat(rotated.refresh().value()).isNotEqualTo(initial.refresh().value());
		assertThat(rotated.refresh().expiresAt()).isAfter(initial.refresh().expiresAt());
		assertThat(java.time.Duration.between(
				flowSessionRepository.findByRequest_Id(request.getId()).orElseThrow().getRefreshExpiresAt(),
				rotated.refresh().expiresAt()).abs().toMillis()).isLessThan(1_000L);
		assertThat(flowSessionService.current(rotated.access().value()).dni()).isEqualTo("00000001");
		assertThatThrownBy(() -> flowSessionService.refresh(initial.refresh().value()))
				.isInstanceOfSatisfying(FlowSessionException.class,
						exception -> assertThat(exception.reason())
								.isEqualTo(FlowSessionException.Reason.REFRESH_CONFLICT));

		flowSessionService.logout(rotated.access().value());
		assertThat(requestRepository.findById(request.getId())).get()
				.extracting(DigitalCredentialRevocationRequestEntity::getRequestStatus)
				.isEqualTo(RevocationRequestStatus.ABANDONED);
		assertThatThrownBy(() -> flowSessionService.current(rotated.access().value()))
				.isInstanceOf(FlowSessionException.class);
	}

	@Test
	void replacesAnUnconsumedIdentityAttemptAndRejectsItsLateCallback() {
		DigitalCredentialRevocationRequestEntity request = new DigitalCredentialRevocationRequestEntity("00000001");
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION);
		request = requestRepository.saveAndFlush(request);

		FlowSessionService.Tokens init = flowSessionService.establish(request.getId());
		URI firstAuthorization = identityService.start(init.access().value(), "first-identity-start");
		String firstState = stateFrom(firstAuthorization);
		URI replacementAuthorization = identityService.start(init.access().value(), "replacement-identity-start");
		String replacementState = stateFrom(replacementAuthorization);

		assertThat(replacementState).isNotEqualTo(firstState);
		List<IdentityVerificationEntity> attempts = identityRepository.findAll().stream()
				.sorted(java.util.Comparator.comparingInt(IdentityVerificationEntity::getAttemptNumber))
				.toList();
		assertThat(attempts).hasSize(2);
		assertThat(attempts.get(0).getVerificationStatus()).isEqualTo(IdentityVerificationStatus.CANCELLED);
		assertThat(attempts.get(0).getErrorOrRevocationCode()).isEqualTo("REPLACED_BY_NEW_ATTEMPT");
		assertThat(attempts.get(0).getPkceVerifierProtected()).isNull();
		assertThat(identityRepository.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId()))
				.get().satisfies(latest -> {
					assertThat(latest.getAttemptNumber()).isEqualTo(2);
					assertThat(latest.getVerificationStatus()).isEqualTo(IdentityVerificationStatus.STARTED);
				});
		assertThatThrownBy(() -> identityService.callback("mock-code", firstState, "mock-session", null))
				.isInstanceOfSatisfying(IdentityIntegrationException.class,
						exception -> assertThat(exception.failure()).isEqualTo(IdentityFailure.STATE_EXPIRED));
		assertThat(identityService.callback("mock-code", replacementState, "mock-session", null).verified()).isTrue();
	}

	@Test
	void verifiedSessionRemainsAtCredentialSelectionWhenNoActiveCredentialsExist() {
		DigitalCredentialRevocationRequestEntity request = new DigitalCredentialRevocationRequestEntity("00000020");
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION);
		request = requestRepository.saveAndFlush(request);
		FlowSessionService.Tokens tokens = flowSessionService.establish(request.getId());
		URI authorization = identityService.start(tokens.access().value(), "verified-empty-list");
		IdentityVerificationService.CallbackResult callback = identityService.callback(
				"mock-code", stateFrom(authorization), "mock-session", null);

		var listed = digitalCredentialListingService.list(request.getId(), "verified-empty-list");
		FlowSessionService.CurrentSession currentSession = flowSessionService.current(callback.accessToken().value());
		IdentityVerificationService.CurrentIdentityStatus currentIdentity =
				identityService.current(callback.accessToken().value());

		assertThat(listed.requestStatus()).isEqualTo("NO_DIGITAL_CREDENTIALS_AVAILABLE");
		assertThat(listed.digitalCredentials()).isEmpty();
		assertThat(currentSession.sessionStatus()).isEqualTo("IDENTITY_VERIFIED");
		assertThat(currentSession.requestStatus()).isEqualTo("NO_DIGITAL_CREDENTIALS_AVAILABLE");
		assertThat(currentSession.nextStep()).isEqualTo("DIGITAL_CREDENTIAL_SELECTION");
		assertThat(currentIdentity.status()).isEqualTo("VERIFIED");
		assertThat(currentIdentity.canContinue()).isTrue();
		assertThat(currentIdentity.nextStep()).isEqualTo("DIGITAL_CREDENTIAL_SELECTION");
	}

	@Test
	void recordsAnExpiredAttemptBeforeStartingItsReplacement() {
		DigitalCredentialRevocationRequestEntity request = new DigitalCredentialRevocationRequestEntity("00000001");
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION);
		request = requestRepository.saveAndFlush(request);
		FlowSessionService.Tokens init = flowSessionService.establish(request.getId());
		identityService.start(init.access().value(), "expiring-identity-start");
		IdentityVerificationEntity first = identityRepository
				.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId()).orElseThrow();
		jdbcTemplate.update("UPDATE identity_verification SET state_expires_at = ? WHERE id = ?",
				java.sql.Timestamp.from(Instant.now().minusSeconds(1)), first.getId());

		identityService.start(init.access().value(), "after-expiration-identity-start");

		assertThat(identityRepository.findById(first.getId())).get().satisfies(expired -> {
			assertThat(expired.getVerificationStatus()).isEqualTo(IdentityVerificationStatus.EXPIRED);
			assertThat(expired.getErrorOrRevocationCode()).isEqualTo("STATE_EXPIRED");
			assertThat(expired.getPkceVerifierProtected()).isNull();
		});
	}

	@Test
	void rejectsReplacementAfterTheCallbackStateWasConsumed() {
		DigitalCredentialRevocationRequestEntity request = new DigitalCredentialRevocationRequestEntity("00000001");
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION);
		request = requestRepository.saveAndFlush(request);
		FlowSessionService.Tokens init = flowSessionService.establish(request.getId());
		URI authorization = identityService.start(init.access().value(), "reserved-identity-start");
		identityPersistence.reserve(identitySecurity.sha256(stateFrom(authorization)), Instant.now());

		assertThatThrownBy(() -> identityService.start(init.access().value(), "conflicting-identity-start"))
				.isInstanceOfSatisfying(IdentityIntegrationException.class,
						exception -> assertThat(exception.failure()).isEqualTo(IdentityFailure.IN_PROGRESS));
		assertThat(identityRepository.findAll()).hasSize(1);
	}

	@Test
	void malformedOptionalCallbackValuesFinishTheAttemptWithoutOverflowingPersistence() {
		DigitalCredentialRevocationRequestEntity request = new DigitalCredentialRevocationRequestEntity("00000001");
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION);
		request = requestRepository.saveAndFlush(request);

		FlowSessionService.Tokens init = flowSessionService.establish(request.getId());
		URI authorization = identityService.start(init.access().value(), "callback-boundary-it");
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
		assertThat(verification.getErrorOrRevocationCode()).isEqualTo("INVALID_CALLBACK");
		assertThat(verification.getProviderSessionState()).isNull();
	}

	@Test
	void userCancellationReturnsSilentlyAndKeepsIdentityVerificationRetryable() throws Exception {
		DigitalCredentialRevocationRequestEntity request = pendingIdentityRequest("00000001");
		FlowSessionService.Tokens init = flowSessionService.establish(request.getId());
		String state = stateFrom(identityService.start(init.access().value(), "user-cancelled-it"));

		HttpResponse<String> callback = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
				.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
						+ "/api/v1/idperu/callback?state="
						+ java.net.URLEncoder.encode(state, StandardCharsets.UTF_8)
						+ "&session_state=mock-session&error=user_cancelled"))
						.GET().build(), HttpResponse.BodyHandlers.ofString());

		assertThat(callback.statusCode()).isEqualTo(HttpStatus.SEE_OTHER.value());
		assertThat(callback.headers().firstValue("Location")).hasValue("http://localhost:3000/revocacion");
		assertThat(callback.headers().allValues("Set-Cookie")).anySatisfy(cookie -> assertThat(cookie)
				.contains("idperu_callback_outcome=", "Max-Age=0", "HttpOnly", "SameSite=Lax")
				.doesNotContain("CANCELLED"));
		assertThat(callback.body()).isEmpty();

		IdentityVerificationEntity cancelled = identityRepository
				.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId()).orElseThrow();
		assertThat(cancelled.getVerificationStatus()).isEqualTo(IdentityVerificationStatus.CANCELLED);
		assertThat(cancelled.getDniMatchResult()).isEqualTo(IdentityMatchResult.NOT_EVALUATED);
		assertThat(cancelled.getErrorOrRevocationCode()).isEqualTo("user_cancelled");
		assertThat(cancelled.getPkceVerifierProtected()).isNull();
		assertThat(requestRepository.findById(request.getId()).orElseThrow().getRequestStatus())
				.isEqualTo(RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION);

		HttpResponse<String> current = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/api/v1/identity-verifications/current"))
				.header("Cookie", "revocacion_access=" + init.access().value()
						+ "; idperu_callback_outcome=CANCELLED")
				.GET().build(), HttpResponse.BodyHandlers.ofString());
		assertThat(current.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(current.body()).contains("\"status\":\"CANCELLED\"", "\"canContinue\":false",
				"\"nextStep\":\"IDENTITY_VERIFICATION\"", "\"callbackOutcome\":null");
		assertThat(current.headers().allValues("Set-Cookie")).anySatisfy(cookie -> assertThat(cookie)
				.contains("idperu_callback_outcome=", "Max-Age=0"));

		String retryState = stateFrom(identityService.start(init.access().value(), "retry-after-cancel-it"));
		assertThat(retryState).isNotEqualTo(state);
		assertThat(identityRepository.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId()))
				.get().extracting(IdentityVerificationEntity::getAttemptNumber).isEqualTo(2);
	}

	@Test
	void accessDeniedRemainsCompatibleWithCancellation() {
		DigitalCredentialRevocationRequestEntity request = pendingIdentityRequest("00000001");
		FlowSessionService.Tokens init = flowSessionService.establish(request.getId());
		String state = stateFrom(identityService.start(init.access().value(), "access-denied-it"));

		IdentityVerificationService.CallbackResult result = identityService.callback(
				null, state, "mock-session", "access_denied");

		assertThat(result.status()).isEqualTo("CANCELLED");
		assertThat(identityRepository.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId()))
				.get().satisfies(cancelled -> {
					assertThat(cancelled.getVerificationStatus()).isEqualTo(IdentityVerificationStatus.CANCELLED);
					assertThat(cancelled.getErrorOrRevocationCode()).isEqualTo("access_denied");
					assertThat(cancelled.getPkceVerifierProtected()).isNull();
				});
	}

	@Test
	void providerRejectionStillProducesTheCitizenFacingOutcome() throws Exception {
		DigitalCredentialRevocationRequestEntity request = pendingIdentityRequest("00000001");
		FlowSessionService.Tokens init = flowSessionService.establish(request.getId());
		String state = stateFrom(identityService.start(init.access().value(), "provider-rejected-it"));

		HttpResponse<String> callback = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
				.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
						+ "/api/v1/idperu/callback?state="
						+ java.net.URLEncoder.encode(state, StandardCharsets.UTF_8)
						+ "&session_state=mock-session&error=login_required"))
						.GET().build(), HttpResponse.BodyHandlers.ofString());

		assertThat(callback.statusCode()).isEqualTo(HttpStatus.SEE_OTHER.value());
		assertThat(callback.headers().firstValue("Location"))
				.hasValue("http://localhost:3000/revocacion?identityOutcome=REJECTED");
		assertThat(callback.headers().allValues("Set-Cookie")).anySatisfy(cookie -> assertThat(cookie)
				.contains("idperu_callback_outcome=REJECTED", "HttpOnly", "SameSite=Lax"));
		assertThat(identityRepository.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId()))
				.get().extracting(IdentityVerificationEntity::getVerificationStatus)
				.isEqualTo(IdentityVerificationStatus.REJECTED);
	}

	@Test
	void httpMockFlowPersistsSelectionAndAuthorizesTheReasonStep() throws Exception {
		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
		HttpResponse<String> initiation = client.send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/api/v1/revocation-requests"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(
						"{\"dni\":\"00000001\",\"recaptchaToken\":\"test-recaptcha-valid\"}"))
				.build(), HttpResponse.BodyHandlers.ofString());
		String initiationCookie = cookiePair(initiation);
		HttpResponse<String> beforeStart = client.send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/api/v1/identity-verifications/current"))
				.header("Cookie", initiationCookie).GET().build(), HttpResponse.BodyHandlers.ofString());

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
		assertThat(callback.headers().firstValue("Location")).hasValue("http://localhost:3000/revocacion");
		String authorizationCookie = cookiePair(callback);

		HttpResponse<String> current = client.send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/api/v1/identity-verifications/current"))
				.header("Cookie", authorizationCookie).GET().build(), HttpResponse.BodyHandlers.ofString());
		HttpResponse<String> currentSession = client.send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/api/v1/session/current"))
				.header("Cookie", authorizationCookie).GET().build(), HttpResponse.BodyHandlers.ofString());

		assertThat(initiation.statusCode()).isEqualTo(200);
		assertThat(beforeStart.statusCode()).isEqualTo(200);
		assertThat(beforeStart.body()).contains("\"status\":\"STARTED\"", "\"canContinue\":false",
				"\"nextStep\":\"IDENTITY_VERIFICATION\"");
		assertThat(start.statusCode()).isEqualTo(200);
		assertThat(current.statusCode()).isEqualTo(200);
		assertThat(current.body()).contains("\"status\":\"VERIFIED\"", "\"canContinue\":true",
				"\"nextStep\":\"DIGITAL_CREDENTIAL_SELECTION\"")
				.doesNotContain("00000001", "mock-code", state);
		assertThat(current.headers().firstValue("Cache-Control")).hasValue("no-store");
		assertThat(currentSession.statusCode()).isEqualTo(200);
		assertThat(currentSession.headers().firstValue("Cache-Control")).hasValue("no-store");
		assertThat(currentSession.body()).contains("\"nextStep\":\"DIGITAL_CREDENTIAL_SELECTION\"");
		assertThat(initiationCookie).isNotEqualTo(authorizationCookie);

		HttpResponse<String> listed = client.send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port
						+ "/api/v1/revocation-requests/current/digital-credentials"))
				.header("Cookie", authorizationCookie).GET().build(), HttpResponse.BodyHandlers.ofString());
		java.util.regex.Matcher digitalCredential = java.util.regex.Pattern
				.compile("\\\"digitalCredentialUuid\\\":\\\"([^\\\"]+)\\\"").matcher(listed.body());
		assertThat(digitalCredential.find()).isTrue();
		String digitalCredentialUuid = digitalCredential.group(1);
		HttpResponse<String> obsoleteSelection = client.send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port
						+ "/api/v1/revocation-requests/current/digital-credential-selection"))
				.header("Cookie", authorizationCookie)
				.header("Origin", "http://localhost:3000")
				.header("Content-Type", "application/json")
				.PUT(HttpRequest.BodyPublishers.ofString(
						"{\"digitalCredentialUuids\":[\"" + digitalCredentialUuid + "\"]}"))
				.build(), HttpResponse.BodyHandlers.ofString());

		HttpResponse<String> preview = client.send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port
						+ "/api/v1/revocation-requests/current/review"))
				.header("Cookie", authorizationCookie)
				.header("Origin", "http://localhost:3000")
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(
						"{\"digitalCredentialUuid\":\"" + digitalCredentialUuid + "\",\"statusListIndex\":123456,\"reasonCode\":\"LOSS\"}"))
				.build(), HttpResponse.BodyHandlers.ofString());
		HttpResponse<String> session = client.send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/api/v1/session/current"))
				.header("Cookie", authorizationCookie).GET().build(), HttpResponse.BodyHandlers.ofString());

		assertThat(listed.statusCode()).isEqualTo(200);
		assertThat(listed.body()).contains("\"requestStatus\":\"DIGITAL_CREDENTIALS_AVAILABLE\"",
				"\"canContinue\":true");
		assertThat(obsoleteSelection.statusCode()).isEqualTo(404);
		assertThat(obsoleteSelection.body()).contains("\"code\":\"RESOURCE_NOT_FOUND\"");
		assertThat(preview.statusCode()).isEqualTo(200);
		assertThat(preview.body()).contains("\"requestStatus\":\"DIGITAL_CREDENTIALS_AVAILABLE\"",
				"\"confirmed\":false");
		assertThat(session.statusCode()).isEqualTo(200);
		assertThat(session.body()).contains("\"requestStatus\":\"DIGITAL_CREDENTIALS_AVAILABLE\"",
				"\"nextStep\":\"DIGITAL_CREDENTIAL_SELECTION\"");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM revocation_request_digital_credential WHERE selected = TRUE", Integer.class))
				.isZero();
	}

	@Test
	void invalidBrowserCallbackRedirectsWithoutExposingTheApiErrorDocument() throws Exception {
		HttpResponse<String> callback = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
				.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
						+ "/api/v1/idperu/callback?code=provider-code&state=unknown-state"))
						.GET().build(), HttpResponse.BodyHandlers.ofString());

		assertThat(callback.statusCode()).isEqualTo(HttpStatus.SEE_OTHER.value());
		assertThat(callback.headers().firstValue("Location"))
				.hasValue("http://localhost:3000/revocacion?identityOutcome=ERROR");
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
		assertThat(callback.headers().firstValue("Location"))
				.hasValue("http://localhost:3000/revocacion?identityOutcome=ERROR");
		assertThat(callback.headers().allValues("Set-Cookie")).anySatisfy(cookie -> assertThat(cookie)
				.contains("idperu_callback_outcome=ERROR", "HttpOnly", "SameSite=Lax")
				.doesNotContain("provider-code", "unknown-state", "provider-session"));
		assertThat(callback.body()).isEmpty();
	}

	@Test
	void cleanDatabaseRunsFlywayCreatesTheDomainTablesAndReportsSafeHealth() throws Exception {
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
				 'technical_code', 'technical_detail', 'public_reference',
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
		Integer singleSelectionIndexCount = jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM information_schema.statistics
				WHERE table_schema = DATABASE()
				  AND table_name = 'revocation_request_digital_credential'
				  AND index_name = 'uq_revocation_request_single_selected'
				""", Integer.class);
		HttpResponse<String> health = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health")).GET().build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(tables).containsExactly(
				"digital_credential_availability_check", "digital_credential_revocation_request",
				"identity_verification", "revocation_audit_event", "revocation_flow_session",
				"revocation_operation", "revocation_receipt", "revocation_request_digital_credential");
		assertThat(obsoleteColumns).isEmpty();
		assertThat(migrationCount).isEqualTo(16);
		assertThat(singleSelectionIndexCount).isEqualTo(1);
		assertThat(tablesWithoutComments).isEmpty();
		assertThat(columnsWithoutComments).isEmpty();
		assertThat(documentedColumnCount).isEqualTo(110);
		assertThat(jdbcTemplate.queryForObject("""
				SELECT COUNT(*) FROM information_schema.statistics
				WHERE table_schema = DATABASE()
				  AND table_name = 'revocation_receipt'
				  AND index_name = 'idx_revocation_receipt_processing'
				""", Integer.class)).isEqualTo(3);
		assertThat(health.statusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(health.body()).contains("\"status\":\"UP\"")
				.doesNotContain("jdbc", "mysql", "username", "password", "sql", "dni");
	}

	@Test
	void exposesExactSpanishCommentsAndPreservesRepresentativeColumnDefinitions() {
		assertThat(tableComment("digital_credential_revocation_request"))
				.isEqualTo("Solicitud ciudadana para revocar una credencial digital");
		assertThat(columnComment("digital_credential_revocation_request", "dni"))
				.isEqualTo("Número de DNI asociado a la solicitud ciudadana");
		assertThat(columnComment("revocation_request_digital_credential", "selected"))
				.isEqualTo("Indica si el ciudadano seleccionó la credencial digital para revocarla");
		assertThat(columnComment("revocation_operation", "idempotency_key"))
				.isEqualTo("Clave única que evita ejecutar dos veces la misma operación técnica");
		assertThat(columnComment("revocation_operation", "normalized_result"))
				.isEqualTo("Resultado general normalizado de la operación técnica");
		assertThat(columnComment("revocation_receipt", "receipt_code"))
				.isEqualTo("Código único asignado a la constancia");
		assertThat(columnComment("revocation_audit_event", "event_type"))
				.isEqualTo("Tipo de evento relevante registrado por el backend");

		Map<String, Object> requestId = columnDefinition("digital_credential_revocation_request", "id");
		assertThat(requestId).containsEntry("column_type", "bigint unsigned")
				.containsEntry("is_nullable", "NO")
				.containsEntry("extra", "auto_increment");

		Map<String, Object> dni = columnDefinition("digital_credential_revocation_request", "dni");
		assertThat(dni).containsEntry("column_type", "char(8)")
				.containsEntry("is_nullable", "NO")
				.containsEntry("character_set_name", "ascii")
				.containsEntry("collation_name", "ascii_bin");

		Map<String, Object> selected = columnDefinition("revocation_request_digital_credential", "selected");
		assertThat(selected).containsEntry("column_type", "tinyint(1)")
				.containsEntry("is_nullable", "NO")
				.containsEntry("column_default", "0");

		Map<String, Object> version = columnDefinition("revocation_request_digital_credential", "version");
		assertThat(version).containsEntry("column_type", "bigint unsigned")
				.containsEntry("is_nullable", "NO")
				.containsEntry("column_default", "0");
	}

	@Test
	void serializesConcurrentAvailabilityAndStartsANewRequestForALaterEntry() throws Exception {
		String body = "{\"dni\":\"00000001\",\"recaptchaToken\":\"test-recaptcha-valid\"}";
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> { start.await(); return postRevocation(body); });
			var second = executor.submit(() -> { start.await(); return postRevocation(body); });
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
				"SELECT COUNT(*) FROM digital_credential_revocation_request WHERE dni='00000001'", Integer.class))
				.isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM digital_credential_availability_check", Integer.class)).isEqualTo(1);
		assertThat(digitalCredentialCount()).isZero();

		Long firstRequestId = jdbcTemplate.queryForObject(
				"SELECT id FROM digital_credential_revocation_request WHERE dni='00000001'", Long.class);
		HttpResponse<String> fresh = postRevocation(body);
		assertThat(fresh.statusCode()).isEqualTo(200);
		assertThat(fresh.body()).contains("\"requestId\":")
				.doesNotContain("reused", "requestReference", "00000001");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM digital_credential_revocation_request WHERE dni='00000001'", Integer.class))
				.isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM digital_credential_availability_check", Integer.class)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT request_status FROM digital_credential_revocation_request WHERE id=?",
				String.class, firstRequestId)).isEqualTo("ABANDONED");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM digital_credential_availability_check WHERE attempt_number=1", Integer.class))
				.isEqualTo(2);
		assertThat(digitalCredentialCount()).isZero();
	}

	@Test
	void blocksProtectedRequestWithoutDisclosingHistoricalData() throws Exception {
		DigitalCredentialRevocationRequestEntity protectedRequest = saveRequest("00000001");
		protectedRequest.transitionTo(RevocationRequestStatus.REVOCATION_IN_PROGRESS, null);
		requestRepository.saveAndFlush(protectedRequest);

		HttpResponse<String> response = postRevocation("{\"dni\":\"00000001\",\"recaptchaToken\":\"test-recaptcha-valid\"}");

		assertThat(response.statusCode()).isEqualTo(409);
		assertThat(response.body()).contains("REVOCATION_REQUEST_IN_PROGRESS", "correlationId")
				.doesNotContain("00000001", "requestId", "constancia", "digitalCredential");
		assertThat(requestRepository.count()).isEqualTo(1);
		assertThat(availabilityRepository.count()).isZero();
	}

	@Test
	void rejectsInvalidDniWithoutPersistenceAndReturnsCorrelation() throws Exception {
		HttpResponse<String> response = postRevocation("{\"dni\":\"1234\",\"recaptchaToken\":\"test-recaptcha-valid\"}");
		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("VALIDATION_ERROR", "correlationId")
				.doesNotContain("1234");
		assertThat(requestRepository.count()).isZero();
		assertThat(availabilityRepository.count()).isZero();
	}

	@Test
	void rejectedCaptchaCreatesNeitherRequestNorAvailabilityAttempt() throws Exception {
		HttpResponse<String> response = postRevocation(
				"{\"dni\":\"00000001\",\"recaptchaToken\":\"test-recaptcha-invalid\"}");

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("RECAPTCHA_REJECTED", "correlationId")
				.doesNotContain("test-recaptcha-invalid", "00000001");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM digital_credential_revocation_request", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM digital_credential_availability_check", Integer.class)).isZero();
	}

	@Test
	void persistsEveryDeterministicAlternativeOutcomeWithControlledHttpSemantics() throws Exception {
		HttpResponse<String> notAvailable = postRevocation("{\"dni\":\"00000002\",\"recaptchaToken\":\"test-recaptcha-valid\"}");
		HttpResponse<String> unavailable = postRevocation("{\"dni\":\"00000003\",\"recaptchaToken\":\"test-recaptcha-valid\"}");
		HttpResponse<String> inconclusive = postRevocation("{\"dni\":\"00000004\",\"recaptchaToken\":\"test-recaptcha-valid\"}");
		HttpResponse<String> technical = postRevocation("{\"dni\":\"00000005\",\"recaptchaToken\":\"test-recaptcha-valid\"}");
		HttpResponse<String> timeout = postRevocation("{\"dni\":\"00000006\",\"recaptchaToken\":\"test-recaptcha-valid\"}");

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
		assertThat(digitalCredentialCount()).isZero();
		assertThat(notAvailable.body() + unavailable.body() + inconclusive.body() + technical.body() + timeout.body())
				.doesNotContain("digitalCredentialUuid", "orderNumber", "emissionCreatedAt", "digitalCredentialCount");
	}

	@Test
	void storesReadableRequestReasonConfirmationAndTerminalHistoryWithoutExpiration() {
		DigitalCredentialRevocationRequestEntity request = saveRequest("12345678");
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION);
		request.confirmDecision(RevocationReasonCode.OTHER, "Cambio de dispositivo personal",
				Instant.now(), "REVOCACION_CREDENCIALES_DIGITALES_V1");
		requestRepository.saveAndFlush(request);

		assertThat(requestRepository.findById(request.getId())).get().satisfies(found -> {
			assertThat(found.getDni()).isEqualTo("12345678");
			assertThat(found.getRequestStatus()).isEqualTo(RevocationRequestStatus.CONFIRMED);
			assertThat(found.getAvailabilityResult()).isEqualTo(CurrentAvailabilityResult.AVAILABLE);
			assertThat(found.getReasonCode()).isEqualTo(RevocationReasonCode.OTHER);
			assertThat(found.getOtherReason()).isEqualTo("Cambio de dispositivo personal");
			assertThat(found.getConfirmedAt()).isNotNull();
		});
		assertThatThrownBy(() -> request.confirmDecision(RevocationReasonCode.LOSS, null,
				Instant.now(), "REVOCACION_CREDENCIALES_DIGITALES_V1"))
				.isInstanceOf(IllegalStateException.class);
		assertThat(requestRepository.findFirstByDniAndRequestStatusInOrderByCreatedAtDesc(
				"12345678", Set.of(RevocationRequestStatus.CONFIRMED))).get()
				.extracting(DigitalCredentialRevocationRequestEntity::getId).isEqualTo(request.getId());
		assertThat(requestRepository.findFirstByDniOrderByCreatedAtDesc("12345678")).get()
				.extracting(DigitalCredentialRevocationRequestEntity::getId).isEqualTo(request.getId());

		DigitalCredentialRevocationRequestEntity historical = saveRequest("12345678");
		historical.transitionTo(RevocationRequestStatus.ABANDONED, RevocationFinalOutcome.ABANDONED);
		requestRepository.saveAndFlush(historical);
		assertThat(requestRepository.count()).isEqualTo(2);
		assertThatThrownBy(() -> new DigitalCredentialRevocationRequestEntity("1234ABCD"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void recordsRepeatableAvailabilityAndIdentityAttemptsAndFindsLatestValid() {
		DigitalCredentialRevocationRequestEntity request = saveRequest("23456789");
		Instant now = Instant.now();
		DigitalCredentialAvailabilityCheckEntity eligibility1 = new DigitalCredentialAvailabilityCheckEntity(
				request, 1, AvailabilityCheckStatus.SUBMITTED, now, CORRELATION);
		eligibility1.complete(AvailabilityCheckResult.INCONCLUSIVE, now.plusSeconds(1), "eligibility-ref-1");
		availabilityRepository.saveAndFlush(eligibility1);
		DigitalCredentialAvailabilityCheckEntity eligibility2 = new DigitalCredentialAvailabilityCheckEntity(
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
				now.plusSeconds(3), "identity-ref-2", null, null, null, "ANA");
		identityRepository.saveAndFlush(identity2);

		assertThat(availabilityRepository.findFirstByRequest_IdOrderByAttemptNumberDesc(request.getId())).get()
				.extracting(DigitalCredentialAvailabilityCheckEntity::getId).isEqualTo(eligibility2.getId());
		assertThat(identityRepository.findFirstByRequest_IdAndVerificationStatusOrderByAttemptNumberDesc(
				request.getId(), IdentityVerificationStatus.VERIFIED)).get()
				.extracting(IdentityVerificationEntity::getId).isEqualTo(identity2.getId());
		assertThatThrownBy(() -> availabilityRepository.saveAndFlush(new DigitalCredentialAvailabilityCheckEntity(
				request, 2, AvailabilityCheckStatus.CREATED, now.plusSeconds(4), CORRELATION)))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> identityRepository.saveAndFlush(new IdentityVerificationEntity(
				request, 2, "ID_PERU", now.plusSeconds(4), CORRELATION)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void keepsUncertainRevocationOnTheSameOperationAndEnforcesIdempotencyAndAttemptUniqueness() {
		DigitalCredentialRevocationRequestEntity request = saveRequest("45678901");
		String key = "revoke-request-001";
		Instant now = Instant.now();
		RevocationOperationEntity uncertain = new RevocationOperationEntity(request, key, 1, now, CORRELATION);
		uncertain.markSubmitted(now.plusSeconds(1), "revocation-ref-1");
		uncertain.complete(RevocationOperationStatus.OUTCOME_UNKNOWN, RevocationResult.OUTCOME_UNKNOWN,
				now.plusSeconds(2), null, null, "PROVIDER_TIMEOUT");
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
		DigitalCredentialRevocationRequestEntity request = saveRequest("56789012");
		Instant now = Instant.now();
		RevocationOperationEntity operation = new RevocationOperationEntity(
				request, "revoke-receipt-001", 1, now, CORRELATION);
		operation.complete(RevocationOperationStatus.SUCCEEDED, RevocationResult.SUCCEEDED,
				now.plusSeconds(1), now.plusSeconds(1), null, null);
		revocationRepository.saveAndFlush(operation);
		request.transitionTo(RevocationRequestStatus.COMPLETED, RevocationFinalOutcome.REVOCATION_SUCCEEDED);
		requestRepository.saveAndFlush(request);

		RevocationReceiptEntity available = new RevocationReceiptEntity(request, operation, "RV-TEST-0001");
		available.markAvailable("documents/receipt-0001", now.plusSeconds(2), now.plusSeconds(3));
		receiptRepository.saveAndFlush(available);
		RevocationReceiptEntity failed = new RevocationReceiptEntity(request, operation, "RV-TEST-0002");
		failed.markFailed("RENDER_FAILURE");
		receiptRepository.saveAndFlush(failed);

		assertThat(receiptRepository.findFirstByRequest_IdAndGenerationStatusOrderByAvailableAtDesc(
				request.getId(), ReceiptGenerationStatus.AVAILABLE)).get()
				.extracting(RevocationReceiptEntity::getId).isEqualTo(available.getId());
		assertThat(requestRepository.findById(request.getId())).get()
				.extracting(DigitalCredentialRevocationRequestEntity::getFinalOutcome)
				.isEqualTo(RevocationFinalOutcome.REVOCATION_SUCCEEDED);
		assertThatThrownBy(() -> receiptRepository.saveAndFlush(
				new RevocationReceiptEntity(request, operation, "RV-TEST-0001")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void appendsOrderedMinimalAuditHistoryAndRejectsOrphanRows() {
		DigitalCredentialRevocationRequestEntity request = saveRequest("67890123");
		Instant now = Instant.now();
		RevocationAuditEventEntity started = auditRepository.save(new RevocationAuditEventEntity(
				request, RevocationAuditEventType.REQUEST_STARTED, null, RevocationRequestStatus.STARTED,
				"CREATED", CORRELATION, AuditEventOrigin.CITIZEN, now));
		RevocationAuditEventEntity checked = auditRepository.save(new RevocationAuditEventEntity(
				request, RevocationAuditEventType.ELIGIBILITY_CHECKED, RevocationRequestStatus.STARTED,
				RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION, "AVAILABLE", CORRELATION,
				AuditEventOrigin.EXTERNAL_PROVIDER, now.plusSeconds(1)));

		assertThat(auditRepository.findByRequest_IdOrderByOccurredAtAscIdAsc(request.getId()))
				.extracting(RevocationAuditEventEntity::getId).containsExactly(started.getId(), checked.getId());
		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO revocation_audit_event
				(request_id, event_type, correlation_id, event_origin, occurred_at)
				VALUES (?, 'REQUEST_STARTED', ?, 'SYSTEM', CURRENT_TIMESTAMP(6))
				""", Long.MAX_VALUE, CORRELATION)).isInstanceOf(DataIntegrityViolationException.class);
	}

	private DigitalCredentialRevocationRequestEntity saveRequest(String dni) {
		return requestRepository.saveAndFlush(new DigitalCredentialRevocationRequestEntity(dni));
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

	private HttpResponse<String> postRevocation(String body) throws Exception {
		return HttpClient.newHttpClient().send(HttpRequest.newBuilder(
				URI.create("http://localhost:" + port + "/api/v1/revocation-requests"))
				.header("Content-Type", "application/json")
				.header("X-Correlation-ID", "eligibility-it")
				.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> currentRequest(String method, String path, String accessToken, String body)
			throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(
				"http://localhost:" + port + "/api/v1/revocation-requests/current" + path))
				.header("Cookie", "revocacion_access=" + accessToken)
				.header("X-Correlation-ID", "confirmation-http-it");
		if (body == null) request.GET();
		else request.header("Content-Type", "application/json")
				.method(method, HttpRequest.BodyPublishers.ofString(body));
		return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	private Integer digitalCredentialCount() {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM revocation_request_digital_credential", Integer.class);
	}

	private static String stateFrom(URI authorization) {
		return java.util.Arrays.stream(authorization.getRawQuery().split("&"))
				.filter(value -> value.startsWith("state="))
				.map(value -> URLDecoder.decode(value.substring(6), StandardCharsets.UTF_8))
				.findFirst().orElseThrow();
	}

	private DigitalCredentialRevocationRequestEntity pendingIdentityRequest(String dni) {
		DigitalCredentialRevocationRequestEntity request = new DigitalCredentialRevocationRequestEntity(dni);
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.PENDING_IDENTITY_VERIFICATION);
		return requestRepository.saveAndFlush(request);
	}

	private static String cookiePair(HttpResponse<?> response) {
		return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
	}
}
