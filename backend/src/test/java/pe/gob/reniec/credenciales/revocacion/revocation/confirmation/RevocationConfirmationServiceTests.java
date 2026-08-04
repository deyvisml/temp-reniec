package pe.gob.reniec.credenciales.revocacion.revocation.confirmation;

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

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.*;

class RevocationConfirmationServiceTests {

	private static final String UUID = "11111111-1111-4111-8111-111111111111";
	private final DigitalCredentialRevocationRequestRepository requests = mock(DigitalCredentialRevocationRequestRepository.class);
	private final RevocationRequestDigitalCredentialRepository digitalCredentials = mock(RevocationRequestDigitalCredentialRepository.class);
	private final IdentityVerificationRepository verifications = mock(IdentityVerificationRepository.class);
	private final RevocationAuditEventRepository auditEvents = mock(RevocationAuditEventRepository.class);
	private final RevocationConfirmationService service = new RevocationConfirmationService(
			provider(requests), provider(digitalCredentials), provider(verifications), provider(auditEvents),
			new RevocationConsentCatalog());

	private DigitalCredentialRevocationRequestEntity request;
	private RevocationRequestDigitalCredentialEntity digitalCredential;

	@BeforeEach
	void setUp() {
		request = new DigitalCredentialRevocationRequestEntity("73905791");
		ReflectionTestUtils.setField(request, "id", 7L);
		request.recordAvailability(CurrentAvailabilityResult.AVAILABLE,
				RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE);

		IdentityVerificationEntity verification = new IdentityVerificationEntity(
				request, 1, "ID_PERU", Instant.now().minusSeconds(30), "identity-correlation");
		verification.finish(IdentityVerificationStatus.VERIFIED, IdentityMatchResult.MATCH,
				Instant.now().minusSeconds(20), "external", null, null, null, "ANA");

		digitalCredential = new RevocationRequestDigitalCredentialEntity(request, 31, "DniPeruanoCredential",
				Instant.parse("2026-07-15T15:24:00Z"), UUID,
				DigitalCredentialAvailabilityStatus.AVAILABLE, null, 0, Instant.now().minusSeconds(10));

		when(requests.findById(7L)).thenReturn(Optional.of(request));
		when(requests.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
		when(verifications.findFirstByRequest_IdOrderByAttemptNumberDesc(7L)).thenReturn(Optional.of(verification));
		when(digitalCredentials.findByRequest_IdOrderByEmissionCreatedAtAscIdAsc(7L))
				.thenReturn(List.of(digitalCredential));
		when(digitalCredentials.findByRequestIdForUpdate(7L)).thenReturn(List.of(digitalCredential));
		when(digitalCredentials.findByRequest_IdAndSelectedTrueOrderByEmissionCreatedAtAscIdAsc(7L))
				.thenAnswer(invocation -> digitalCredential.isSelected() ? List.of(digitalCredential) : List.of());
	}

	@Test
	void previewsAnAuthoritativeSummaryWithoutPersistingTheDraft() {
		RevocationReviewResponse response = service.preview(7L,
				new RevocationReviewRequest(UUID, 31, RevocationReasonCode.THEFT, null));

		assertThat(response.maskedDni()).isEqualTo("******91");
		assertThat(response.firstName()).isEqualTo("ANA");
		assertThat(response.reasonLabel()).isEqualTo("Robo");
		assertThat(response.digitalCredential().statusListIndex()).isEqualTo(31);
		assertThat(response.confirmed()).isFalse();
		assertThat(request.getRequestStatus()).isEqualTo(RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE);
		assertThat(request.getReasonCode()).isNull();
		assertThat(digitalCredential.isSelected()).isFalse();
		verifyNoInteractions(auditEvents);
	}

	@Test
	void normalizesOtherReasonOnlyForThePreviewResponse() {
		RevocationReviewResponse response = service.preview(7L,
				new RevocationReviewRequest(UUID, 31, RevocationReasonCode.OTHER,
						"  Ya no utilizaré el dispositivo asociado  "));

		assertThat(response.otherReason()).isEqualTo("Ya no utilizaré el dispositivo asociado");
		assertThat(request.getOtherReason()).isNull();
	}

	@Test
	void confirmsTheWholeDecisionAndKeepsTheSameEvidenceOnRetry() {
		RevocationConfirmationRequest command = command(UUID, RevocationReasonCode.THEFT, null);

		RevocationReviewResponse first = service.confirm(7L, command, "confirmation-correlation");
		Instant persistedTime = request.getConfirmedAt();
		RevocationReviewResponse repeated = service.confirm(7L, command, "confirmation-correlation-2");

		assertThat(first.confirmed()).isTrue();
		assertThat(repeated.confirmedAt()).isEqualTo(persistedTime);
		assertThat(request.getRequestStatus()).isEqualTo(RevocationRequestStatus.CONFIRMED);
		assertThat(request.getReasonCode()).isEqualTo(RevocationReasonCode.THEFT);
		assertThat(digitalCredential.isSelected()).isTrue();
		assertThat(digitalCredential.getSelectedAt()).isEqualTo(persistedTime);
		verify(auditEvents, times(1)).save(any(RevocationAuditEventEntity.class));
	}

	@Test
	void rejectsAConflictingRetry() {
		service.confirm(7L, command(UUID, RevocationReasonCode.THEFT, null), "first");

		assertThatThrownBy(() -> service.confirm(7L,
				command(UUID, RevocationReasonCode.LOSS, null), "second"))
				.isInstanceOfSatisfying(RevocationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								RevocationConfirmationException.Reason.CONFLICT));
		assertThat(request.getReasonCode()).isEqualTo(RevocationReasonCode.THEFT);
		verify(auditEvents, times(1)).save(any(RevocationAuditEventEntity.class));
	}

	@Test
	void rejectsMissingOrObsoleteConsentWithoutChangingTheRequest() {
		assertThatThrownBy(() -> service.confirm(7L,
				new RevocationConfirmationRequest(UUID, 31, RevocationReasonCode.THEFT, null,
						false, RevocationConsentCatalog.VERSION), "correlation"))
				.isInstanceOfSatisfying(RevocationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								RevocationConfirmationException.Reason.CONSENT_REQUIRED));
		assertThatThrownBy(() -> service.confirm(7L,
				new RevocationConfirmationRequest(UUID, 31, RevocationReasonCode.THEFT, null,
						true, "OLD_VERSION"), "correlation"))
				.isInstanceOfSatisfying(RevocationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								RevocationConfirmationException.Reason.CONSENT_CHANGED));
		assertThat(request.getConfirmedAt()).isNull();
		assertThat(digitalCredential.isSelected()).isFalse();
		verifyNoInteractions(auditEvents);
	}

	@Test
	void rejectsInvalidDraftsWithoutChangingTheRequest() {
		assertThatThrownBy(() -> service.preview(7L,
				new RevocationReviewRequest("22222222-2222-4222-8222-222222222222",
						31, RevocationReasonCode.THEFT, null)))
				.isInstanceOfSatisfying(RevocationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								RevocationConfirmationException.Reason.INVALID_SELECTION));

		assertThatThrownBy(() -> service.preview(7L,
				new RevocationReviewRequest(UUID, 31, RevocationReasonCode.OTHER, "corto")))
				.isInstanceOfSatisfying(RevocationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								RevocationConfirmationException.Reason.INVALID_REASON));
		assertThat(request.getReasonCode()).isNull();
		assertThat(digitalCredential.isSelected()).isFalse();
	}

	@Test
	void resolvesAndComparesASelectionByUuidAndStatusListIndex() {
		RevocationRequestDigitalCredentialEntity otherIndex = new RevocationRequestDigitalCredentialEntity(
				request, 32, "DniPeruanoCredential", Instant.parse("2026-07-16T15:24:00Z"), UUID,
				DigitalCredentialAvailabilityStatus.AVAILABLE, null, 0, Instant.now().minusSeconds(9));
		when(digitalCredentials.findByRequest_IdOrderByEmissionCreatedAtAscIdAsc(7L))
				.thenReturn(List.of(digitalCredential, otherIndex));
		when(digitalCredentials.findByRequestIdForUpdate(7L))
				.thenReturn(List.of(digitalCredential, otherIndex));

		RevocationReviewResponse preview = service.preview(7L,
				new RevocationReviewRequest(UUID, 32, RevocationReasonCode.LOSS, null));
		assertThat(preview.digitalCredential().statusListIndex()).isEqualTo(32);

		service.confirm(7L, new RevocationConfirmationRequest(UUID, 32, RevocationReasonCode.LOSS,
				null, true, RevocationConsentCatalog.VERSION), "tuple-confirmation");

		assertThat(otherIndex.isSelected()).isTrue();
		assertThat(digitalCredential.isSelected()).isFalse();
		assertThatThrownBy(() -> service.confirm(7L,
				new RevocationConfirmationRequest(UUID, 31, RevocationReasonCode.LOSS, null,
						true, RevocationConsentCatalog.VERSION), "tuple-conflict"))
				.isInstanceOfSatisfying(RevocationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								RevocationConfirmationException.Reason.CONFLICT));
	}

	@Test
	void rejectsARevokedDigitalCredentialEvenWhenItsUuidBelongsToTheRequest() {
		Instant revokedAt = Instant.parse("2026-07-20T12:00:00Z");
		RevocationRequestDigitalCredentialEntity revoked = new RevocationRequestDigitalCredentialEntity(
				request, 31, "DniPeruanoCredential", Instant.parse("2026-07-15T15:24:00Z"), UUID,
				DigitalCredentialAvailabilityStatus.REVOKED, revokedAt, 1, revokedAt.plusSeconds(1));
		when(digitalCredentials.findByRequest_IdOrderByEmissionCreatedAtAscIdAsc(7L))
				.thenReturn(List.of(revoked));

		assertThatThrownBy(() -> service.preview(7L,
				new RevocationReviewRequest(UUID, 31, RevocationReasonCode.THEFT, null)))
				.isInstanceOfSatisfying(RevocationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								RevocationConfirmationException.Reason.INVALID_SELECTION));
	}

	@Test
	void rejectsPreviewAndConfirmationWhenTheHistoricalIdentityHasNoName() {
		IdentityVerificationEntity legacy = new IdentityVerificationEntity(
				request, 2, "ID_PERU", Instant.now().minusSeconds(10), "legacy-identity");
		legacy.finish(IdentityVerificationStatus.VERIFIED, IdentityMatchResult.MATCH,
				Instant.now().minusSeconds(5), "external", null, null, null, "ANA");
		ReflectionTestUtils.setField(legacy, "verifiedFirstName", null);
		when(verifications.findFirstByRequest_IdOrderByAttemptNumberDesc(7L))
				.thenReturn(Optional.of(legacy));

		assertThatThrownBy(() -> service.preview(7L,
				new RevocationReviewRequest(UUID, 31, RevocationReasonCode.THEFT, null)))
				.isInstanceOfSatisfying(RevocationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								RevocationConfirmationException.Reason.IDENTITY_REQUIRED));
		assertThatThrownBy(() -> service.confirm(7L,
				command(UUID, RevocationReasonCode.THEFT, null), "correlation"))
				.isInstanceOfSatisfying(RevocationConfirmationException.class,
						error -> assertThat(error.reason()).isEqualTo(
								RevocationConfirmationException.Reason.IDENTITY_REQUIRED));
	}

	private static RevocationConfirmationRequest command(String uuid,
			RevocationReasonCode reason, String otherReason) {
		return new RevocationConfirmationRequest(uuid, 31, reason, otherReason, true,
				RevocationConsentCatalog.VERSION);
	}

	@SuppressWarnings("unchecked")
	private static <T> ObjectProvider<T> provider(T value) {
		ObjectProvider<T> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(value);
		return provider;
	}
}
