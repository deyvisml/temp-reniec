package pe.gob.reniec.certificados.cancelacion.cancellation.certificates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CertificateListingServiceTests {

	private static final Long REQUEST_ID = 41L;
	private static final String CORRELATION_ID = "certificate-unit-test";

	@Test
	void normalizesValidProviderDataBeforeCompletingTheReservation() {
		CertificateListingPort provider = mock(CertificateListingPort.class);
		CertificateListingPersistenceCoordinator persistence = preparedCoordinator();
		when(provider.listCertificates("12345678", CORRELATION_ID)).thenReturn(success(List.of(
				new CertificateListingResult.ListedCertificate(" ORD-001 ", Instant.parse("2025-01-01T10:00:00Z"),
						"11111111-1111-4111-8111-111111111111"))));
		when(persistence.complete(eq(REQUEST_ID), any(), eq(CORRELATION_ID))).thenReturn(List.of());

		new CertificateListingService(provider, properties(), persistence).list(REQUEST_ID, CORRELATION_ID);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<CertificateListingResult.ListedCertificate>> captor = ArgumentCaptor.forClass(List.class);
		verify(persistence).complete(eq(REQUEST_ID), captor.capture(), eq(CORRELATION_ID));
		assertThat(captor.getValue()).singleElement().satisfies(certificate -> {
			assertThat(certificate.orderNumber()).isEqualTo("ORD-001");
			assertThat(certificate.certificateUuid()).isEqualTo("11111111-1111-4111-8111-111111111111");
		});
	}

	@Test
	void rejectsDuplicateOrInvalidProviderCertificatesAndRestoresTheRequest() {
		CertificateListingPersistenceCoordinator persistence = preparedCoordinator();
		CertificateListingPort provider = (dni, correlation) -> success(List.of(
				listed("ORD-001", "11111111-1111-4111-8111-111111111111"),
				listed("ORD-002", "11111111-1111-4111-8111-111111111111")));

		assertThatThrownBy(() -> new CertificateListingService(provider, properties(), persistence)
				.list(REQUEST_ID, CORRELATION_ID))
				.isInstanceOf(CertificateListingException.class)
				.extracting(error -> ((CertificateListingException) error).reason())
				.isEqualTo(CertificateListingException.Reason.INVALID_PROVIDER_RESPONSE);
		verify(persistence).restoreAfterFailure(REQUEST_ID, "INVALID_PROVIDER_RESPONSE", CORRELATION_ID);
	}

	@Test
	void mapsTechnicalProviderOutcomesWithoutTreatingThemAsAnEmptyList() {
		assertProviderFailure(CertificateListingResult.Outcome.TIMEOUT,
				CertificateListingException.Reason.TIMEOUT);
		assertProviderFailure(CertificateListingResult.Outcome.UNAVAILABLE,
				CertificateListingException.Reason.UNAVAILABLE);
		assertProviderFailure(CertificateListingResult.Outcome.MALFORMED,
				CertificateListingException.Reason.INVALID_PROVIDER_RESPONSE);
	}

	@Test
	void deterministicFixturesCoverTheDocumentedScenariosAndDefaultToSuccess() {
		DeterministicCertificateListingAdapter adapter = new DeterministicCertificateListingAdapter();

		assertThat(adapter.listCertificates("00000020", CORRELATION_ID).certificates()).isEmpty();
		assertThat(adapter.listCertificates("00000021", CORRELATION_ID).certificates()).hasSize(1);
		assertThat(adapter.listCertificates("00000022", CORRELATION_ID).certificates()).hasSize(3);
		assertThat(adapter.listCertificates("87654321", CORRELATION_ID).certificates()).hasSize(3);
		assertThat(adapter.listCertificates("00000025", CORRELATION_ID).outcome())
				.isEqualTo(CertificateListingResult.Outcome.TIMEOUT);
		assertThat(adapter.listCertificates("00000026", CORRELATION_ID).outcome())
				.isEqualTo(CertificateListingResult.Outcome.UNAVAILABLE);
		assertThat(adapter.listCertificates("00000027", CORRELATION_ID).outcome())
				.isEqualTo(CertificateListingResult.Outcome.MALFORMED);
	}

	private void assertProviderFailure(CertificateListingResult.Outcome outcome,
			CertificateListingException.Reason expected) {
		CertificateListingPersistenceCoordinator persistence = preparedCoordinator();
		CertificateListingPort provider = (dni, correlation) ->
				new CertificateListingResult(outcome, List.of(), null, "MOCK_FAILURE");

		assertThatThrownBy(() -> new CertificateListingService(provider, properties(), persistence)
				.list(REQUEST_ID, CORRELATION_ID))
				.isInstanceOf(CertificateListingException.class)
				.extracting(error -> ((CertificateListingException) error).reason()).isEqualTo(expected);
		verify(persistence).restoreAfterFailure(REQUEST_ID, expected.name(), CORRELATION_ID);
	}

	private static CertificateListingPersistenceCoordinator preparedCoordinator() {
		CertificateListingPersistenceCoordinator persistence = mock(CertificateListingPersistenceCoordinator.class);
		when(persistence.prepare(eq(REQUEST_ID), eq(CORRELATION_ID), any()))
				.thenReturn(new CertificateListingPersistenceCoordinator.Preparation(
						REQUEST_ID, "12345678", List.of(),
						pe.gob.reniec.certificados.cancelacion.cancellation.persistence.CancellationRequestStatus.IDENTITY_VERIFIED,
						true));
		return persistence;
	}

	private static CertificateListingProperties properties() {
		return new CertificateListingProperties();
	}

	private static CertificateListingResult success(
			List<CertificateListingResult.ListedCertificate> certificates) {
		return new CertificateListingResult(CertificateListingResult.Outcome.SUCCESS,
				certificates, "mock-reference", null);
	}

	private static CertificateListingResult.ListedCertificate listed(String order, String uuid) {
		return new CertificateListingResult.ListedCertificate(
				order, Instant.parse("2025-01-01T10:00:00Z"), uuid);
	}
}
