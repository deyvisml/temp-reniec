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

	private static final String UUID = "11111111-1111-4111-8111-111111111111";
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
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				CancellationRequestStatus.CERTIFICATES_AVAILABLE);

		IdentityVerificationEntity verification = new IdentityVerificationEntity(
				request, 1, "ID_PERU", Instant.now().minusSeconds(30), "identity-correlation");
		verification.finish(IdentityVerificationStatus.VERIFIED, IdentityMatchResult.MATCH,
				Instant.now().minusSeconds(20), "external", null);

		certificate = new CancellationRequestCertificateEntity(request, "0000123456",
				Instant.parse("2026-07-15T15:24:00Z"), UUID, Instant.now().minusSeconds(10));

		when(requests.findById(7L)).thenReturn(Optional.of(request));
		when(requests.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
		when(verifications.findFirstByRequest_IdOrderByAttemptNumberDesc(7L)).thenReturn(Optional.of(verification));
		when(certificates.findByRequest_IdOrderByEmissionCreatedAtAscIdAsc(7L))
				.thenReturn(List.of(certificate));
		when(certificates.findByRequestIdForUpdate(7L)).thenReturn(List.of(certificate));
		when(certificates.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(7L))
				.thenAnswer(invocation -> certificate.isSelected() ? List.of(certificate) : List.of());
	}

	@Test
	void previewsAnAuthoritativeSummaryWithoutPersistingTheDraft() {
		CancellationReviewResponse response = service.preview(7L,
				new CancellationReviewRequest(UUID, CancellationReasonCode.THEFT, null));

		assertThat(response.maskedDni()).isEqualTo("******91");
		assertThat(response.reasonLabel()).isEqualTo("Robo");
		assertThat(response.certificate().orderNumber()).isEqualTo("0000123456");
		assertThat(response.confirmed()).isFalse();
		assertThat(request.getRequestStatus()).isEqualTo(CancellationRequestStatus.CERTIFICATES_AVAILABLE);
		assertThat(request.getReasonCode()).isNull();
		assertThat(certificate.isSelected()).isFalse();
		verifyNoInteractions(auditEvents);
	}

	@Test
	void normalizesOtherReasonOnlyForThePreviewResponse() {
		CancellationReviewResponse response = service.preview(7L,
				new CancellationReviewRequest(UUID, CancellationReasonCode.OTHER,
						"  Ya no utilizaré el dispositivo asociado  "));

		assertThat(response.otherReason()).isEqualTo("Ya no utilizaré el dispositivo asociado");
		assertThat(request.getOtherReason()).isNull();
	}

	@Test
	void confirmsTheWholeDecisionAndKeepsTheSameEvidenceOnRetry() {
		CancellationConfirmationRequest command = command(UUID, CancellationReasonCode.THEFT, null);

		CancellationReviewResponse first = service.confirm(7L, command, "confirmation-correlation");
		Instant persistedTime = request.getConfirmedAt();
		CancellationReviewResponse repeated = service.confirm(7L, command, "confirmation-correlation-2");

		assertThat(first.confirmed()).isTrue();
		assertThat(repeated.confirmedAt()).isEqualTo(persistedTime);
		assertThat(request.getRequestStatus()).isEqualTo(CancellationRequestStatus.CONFIRMED);
		assertThat(request.getReasonCode()).isEqualTo(CancellationReasonCode.THEFT);
		assertThat(certificate.isSelected()).isTrue();
		assertThat(certificate.getSelectedAt()).isEqualTo(persistedTime);
		verify(auditEvents, times(1)).save(any(CancellationAuditEventEntity.class));
	}

	@Test
	void rejectsAConflictingRetry() {
		service.confirm(7L, command(UUID, CancellationReasonCode.THEFT, null), "first");

		assertThatThrownBy(() -> service.confirm(7L,
				command(UUID, CancellationReasonCode.LOSS, null), "second"))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								CancellationConfirmationException.Reason.CONFLICT));
		assertThat(request.getReasonCode()).isEqualTo(CancellationReasonCode.THEFT);
		verify(auditEvents, times(1)).save(any(CancellationAuditEventEntity.class));
	}

	@Test
	void rejectsMissingOrObsoleteConsentWithoutChangingTheRequest() {
		assertThatThrownBy(() -> service.confirm(7L,
				new CancellationConfirmationRequest(UUID, CancellationReasonCode.THEFT, null,
						false, CancellationConsentCatalog.VERSION), "correlation"))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								CancellationConfirmationException.Reason.CONSENT_REQUIRED));
		assertThatThrownBy(() -> service.confirm(7L,
				new CancellationConfirmationRequest(UUID, CancellationReasonCode.THEFT, null,
						true, "OLD_VERSION"), "correlation"))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								CancellationConfirmationException.Reason.CONSENT_CHANGED));
		assertThat(request.getConfirmedAt()).isNull();
		assertThat(certificate.isSelected()).isFalse();
		verifyNoInteractions(auditEvents);
	}

	@Test
	void rejectsInvalidDraftsWithoutChangingTheRequest() {
		assertThatThrownBy(() -> service.preview(7L,
				new CancellationReviewRequest("22222222-2222-4222-8222-222222222222",
						CancellationReasonCode.THEFT, null)))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								CancellationConfirmationException.Reason.INVALID_SELECTION));

		assertThatThrownBy(() -> service.preview(7L,
				new CancellationReviewRequest(UUID, CancellationReasonCode.OTHER, "corto")))
				.isInstanceOfSatisfying(CancellationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								CancellationConfirmationException.Reason.INVALID_REASON));
		assertThat(request.getReasonCode()).isNull();
		assertThat(certificate.isSelected()).isFalse();
	}

	private static CancellationConfirmationRequest command(String uuid,
			CancellationReasonCode reason, String otherReason) {
		return new CancellationConfirmationRequest(uuid, reason, otherReason, true,
				CancellationConsentCatalog.VERSION);
	}

	@SuppressWarnings("unchecked")
	private static <T> ObjectProvider<T> provider(T value) {
		ObjectProvider<T> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(value);
		return provider;
	}
}
