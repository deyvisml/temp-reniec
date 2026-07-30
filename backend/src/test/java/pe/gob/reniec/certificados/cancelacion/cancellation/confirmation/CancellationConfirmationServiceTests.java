package pe.gob.reniec.certificados.cancelacion.cancellation.confirmation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import pe.gob.reniec.certificados.cancelacion.cancellation.persistence.*;

class CancellationConfirmationServiceTests {

	private final CertificateCancellationRequestRepository requests = mock(CertificateCancellationRequestRepository.class);
	private final CancellationRequestCertificateRepository certificates = mock(CancellationRequestCertificateRepository.class);
	private final IdentityVerificationRepository verifications = mock(IdentityVerificationRepository.class);
	private final CancellationAuditEventRepository auditEvents = mock(CancellationAuditEventRepository.class);
	private final CancellationConfirmationService service = new CancellationConfirmationService(
			provider(requests), provider(certificates), provider(verifications), provider(auditEvents),
			new CancellationConsentCatalog());

	private CertificateCancellationRequestEntity request;
	private CancellationRequestCertificateEntity certificate;

	@BeforeEach
	void setUp() {
		request = new CertificateCancellationRequestEntity("73905791");
		ReflectionTestUtils.setField(request, "id", 7L);
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE, CancellationRequestStatus.CERTIFICATES_SELECTED);
		request.registerReason(CancellationReasonCode.THEFT, null);

		IdentityVerificationEntity verification = new IdentityVerificationEntity(
				request, 1, "ID_PERU", Instant.now().minusSeconds(30), "identity-correlation");
		verification.finish(IdentityVerificationStatus.VERIFIED, IdentityMatchResult.MATCH,
				Instant.now().minusSeconds(20), "external", null);

		certificate = new CancellationRequestCertificateEntity(request, "0000123456",
				Instant.parse("2026-07-15T15:24:00Z"), "11111111-1111-4111-8111-111111111111",
				Instant.now().minusSeconds(10));
		certificate.select(Instant.now().minusSeconds(5));

		when(requests.findById(7L)).thenReturn(Optional.of(request));
		when(requests.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
		when(verifications.findFirstByRequest_IdOrderByAttemptNumberDesc(7L)).thenReturn(Optional.of(verification));
		when(certificates.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(7L))
				.thenReturn(List.of(certificate));
		when(certificates.findByRequestIdForUpdate(7L)).thenReturn(List.of(certificate));
	}

	@Test
	void buildsAnAuthoritativeMinimizedSummary() {
		CancellationReviewResponse response = service.review(7L);

		assertThat(response.maskedDni()).isEqualTo("******91");
		assertThat(response.reasonLabel()).isEqualTo("Robo");
		assertThat(response.certificate()).satisfies(item -> {
			assertThat(item.orderNumber()).isEqualTo("0000123456");
			assertThat(item.maskedUuid()).isEqualTo("11111111…1111");
			assertThat(item.maskedUuid()).doesNotContain("-1111-4111-");
		});
	}

	@Test
	void includesThePersistedDescriptionForOtherReason() {
		request.registerReason(CancellationReasonCode.OTHER, "Ya no utilizaré el dispositivo asociado");

		CancellationReviewResponse response = service.review(7L);

		assertThat(response.reasonLabel()).isEqualTo("Otro motivo");
		assertThat(response.otherReason()).isEqualTo("Ya no utilizaré el dispositivo asociado");
	}

	@Test
	void rejectsAnInvalidStateOrAnUnverifiedIdentity() {
		request.transitionTo(CancellationRequestStatus.CERTIFICATES_SELECTED, null);
		assertThatThrownBy(() -> service.review(7L))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								CancellationConfirmationException.Reason.NOT_ALLOWED));

		request.registerReason(CancellationReasonCode.THEFT, null);
		when(verifications.findFirstByRequest_IdOrderByAttemptNumberDesc(7L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.review(7L))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								CancellationConfirmationException.Reason.IDENTITY_REQUIRED));
	}

	@Test
	void rejectsWhenTheLatestIdentityAttemptIsNotVerified() {
		IdentityVerificationEntity rejected = new IdentityVerificationEntity(
				request, 2, "ID_PERU", Instant.now().minusSeconds(10), "latest-correlation");
		rejected.finish(IdentityVerificationStatus.REJECTED, IdentityMatchResult.MISMATCH,
				Instant.now().minusSeconds(5), null, "IDENTITY_MISMATCH");
		when(verifications.findFirstByRequest_IdOrderByAttemptNumberDesc(7L))
				.thenReturn(Optional.of(rejected));

		assertThatThrownBy(() -> service.review(7L))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								CancellationConfirmationException.Reason.IDENTITY_REQUIRED));
	}

	@Test
	void confirmsOnceAndKeepsTheSameEvidenceOnRetry() {
		CancellationConfirmationRequest command = new CancellationConfirmationRequest(
				true, CancellationConsentCatalog.VERSION);

		CancellationReviewResponse first = service.confirm(7L, command, "confirmation-correlation");
		Instant persistedTime = request.getConfirmedAt();
		CancellationReviewResponse repeated = service.confirm(7L, command, "confirmation-correlation-2");

		assertThat(first.confirmed()).isTrue();
		assertThat(repeated.confirmedAt()).isEqualTo(persistedTime);
		assertThat(request.getConsentVersion()).isEqualTo(CancellationConsentCatalog.VERSION);
		verify(auditEvents, times(1)).save(any(CancellationAuditEventEntity.class));
	}

	@Test
	void rejectsMissingOrObsoleteConsentWithoutChangingTheRequest() {
		assertThatThrownBy(() -> service.confirm(7L,
				new CancellationConfirmationRequest(false, CancellationConsentCatalog.VERSION), "correlation"))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
							CancellationConfirmationException.Reason.CONSENT_REQUIRED));
		assertThatThrownBy(() -> service.confirm(7L,
				new CancellationConfirmationRequest(true, "OLD_VERSION"), "correlation"))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
							CancellationConfirmationException.Reason.CONSENT_CHANGED));
		assertThat(request.getConfirmedAt()).isNull();
		verifyNoInteractions(auditEvents);
	}

	@Test
	void rejectsASelectedCertificateFromAnotherRequest() {
		CertificateCancellationRequestEntity foreign = new CertificateCancellationRequestEntity("00000002");
		ReflectionTestUtils.setField(foreign, "id", 8L);
		CancellationRequestCertificateEntity injected = new CancellationRequestCertificateEntity(foreign,
				"FOREIGN", Instant.now().minusSeconds(30), "22222222-2222-4222-8222-222222222222",
				Instant.now().minusSeconds(20));
		injected.select(Instant.now().minusSeconds(10));
		when(certificates.findByRequestIdForUpdate(7L)).thenReturn(List.of(injected));

		assertThatThrownBy(() -> service.confirm(7L,
				new CancellationConfirmationRequest(true, CancellationConsentCatalog.VERSION), "correlation"))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
							CancellationConfirmationException.Reason.INVALID_SELECTION));
		assertThat(request.getConfirmedAt()).isNull();
	}

	@Test
	void rejectsAReviewWithZeroOrMultipleSelectedCertificates() {
		when(certificates.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(7L))
				.thenReturn(List.of());
		assertThatThrownBy(() -> service.review(7L))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								CancellationConfirmationException.Reason.INVALID_SELECTION));

		CancellationRequestCertificateEntity second = new CancellationRequestCertificateEntity(request,
				"0000123457", Instant.parse("2026-07-16T15:24:00Z"),
				"22222222-2222-4222-8222-222222222222", Instant.now().minusSeconds(9));
		second.select(Instant.now().minusSeconds(4));
		when(certificates.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(7L))
				.thenReturn(List.of(certificate, second));
		assertThatThrownBy(() -> service.review(7L))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								CancellationConfirmationException.Reason.INVALID_SELECTION));
	}

	@Test
	void rejectsConfirmationWithMultiplePersistedSelections() {
		CancellationRequestCertificateEntity second = new CancellationRequestCertificateEntity(request,
				"0000123457", Instant.parse("2026-07-16T15:24:00Z"),
				"22222222-2222-4222-8222-222222222222", Instant.now().minusSeconds(9));
		second.select(Instant.now().minusSeconds(4));
		when(certificates.findByRequestIdForUpdate(7L)).thenReturn(List.of(certificate, second));

		assertThatThrownBy(() -> service.confirm(7L,
				new CancellationConfirmationRequest(true, CancellationConsentCatalog.VERSION), "correlation"))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								CancellationConfirmationException.Reason.INVALID_SELECTION));
		assertThat(request.getConfirmedAt()).isNull();
	}

	@SuppressWarnings("unchecked")
	private static <T> ObjectProvider<T> provider(T value) {
		ObjectProvider<T> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(value);
		return provider;
	}
}
