package pe.gob.reniec.credenciales.revocacion.revocation.digitalcredentials;

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

import pe.gob.reniec.credenciales.revocacion.revocation.persistence.DigitalCredentialAvailabilityStatus;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestDigitalCredentialEntity;
import pe.gob.reniec.credenciales.revocacion.revocation.persistence.RevocationRequestStatus;

class DigitalCredentialListingServiceTests {

	private static final Long REQUEST_ID = 41L;
	private static final String CORRELATION_ID = "digitalCredential-unit-test";

	@Test
	void normalizesValidProviderDataBeforeCompletingTheReservation() {
		DigitalCredentialListingPort provider = mock(DigitalCredentialListingPort.class);
		DigitalCredentialListingPersistenceCoordinator persistence = preparedCoordinator();
		when(provider.listDigitalCredentials("12345678", CORRELATION_ID)).thenReturn(success(List.of(
				new DigitalCredentialListingResult.ListedDigitalCredential(31, " DniPeruanoCredential ",
						Instant.parse("2025-01-01T10:00:00Z"),
						"11111111-1111-4111-8111-111111111111", DigitalCredentialStatus.ACTIVE, null, 0))));
		when(persistence.complete(eq(REQUEST_ID), any(), eq(CORRELATION_ID))).thenReturn(List.of());

		new DigitalCredentialListingService(provider, properties(), persistence).list(REQUEST_ID, CORRELATION_ID);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DigitalCredentialListingResult.ListedDigitalCredential>> captor = ArgumentCaptor.forClass(List.class);
		verify(persistence).complete(eq(REQUEST_ID), captor.capture(), eq(CORRELATION_ID));
		assertThat(captor.getValue()).singleElement().satisfies(digitalCredential -> {
			assertThat(digitalCredential.statusListIndex()).isEqualTo(31);
			assertThat(digitalCredential.credentialType()).isEqualTo("DniPeruanoCredential");
			assertThat(digitalCredential.digitalCredentialUuid()).isEqualTo("11111111-1111-4111-8111-111111111111");
		});
	}

	@Test
	void acceptsRepeatedUuidsWithDifferentIndexes() {
		DigitalCredentialListingPersistenceCoordinator persistence = preparedCoordinator();
		DigitalCredentialListingPort provider = (dni, correlation) -> success(List.of(
				listed(31, "11111111-1111-4111-8111-111111111111"),
				listed(32, "11111111-1111-4111-8111-111111111111")));
		when(persistence.complete(eq(REQUEST_ID), any(), eq(CORRELATION_ID))).thenReturn(List.of());

		new DigitalCredentialListingService(provider, properties(), persistence)
				.list(REQUEST_ID, CORRELATION_ID);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<DigitalCredentialListingResult.ListedDigitalCredential>> captor =
				ArgumentCaptor.forClass(List.class);
		verify(persistence).complete(eq(REQUEST_ID), captor.capture(), eq(CORRELATION_ID));
		assertThat(captor.getValue())
				.extracting(DigitalCredentialListingResult.ListedDigitalCredential::statusListIndex)
				.containsExactly(31, 32);
		assertThat(captor.getValue())
				.extracting(DigitalCredentialListingResult.ListedDigitalCredential::digitalCredentialUuid)
				.containsOnly("11111111-1111-4111-8111-111111111111");
	}

	@Test
	void rejectsRepeatedIndexesAndExactTuples() {
		assertInvalid(List.of(
				listed(31, "11111111-1111-4111-8111-111111111111"),
				listed(31, "22222222-2222-4222-8222-222222222222")));
		assertInvalid(List.of(
				listed(31, "11111111-1111-4111-8111-111111111111"),
				listed(31, "11111111-1111-4111-8111-111111111111")));
	}

	@Test
	void mapsTechnicalProviderOutcomesWithoutTreatingThemAsAnEmptyList() {
		assertProviderFailure(DigitalCredentialListingResult.Outcome.TIMEOUT,
				DigitalCredentialListingException.Reason.TIMEOUT);
		assertProviderFailure(DigitalCredentialListingResult.Outcome.UNAVAILABLE,
				DigitalCredentialListingException.Reason.UNAVAILABLE);
		assertProviderFailure(DigitalCredentialListingResult.Outcome.MALFORMED,
				DigitalCredentialListingException.Reason.INVALID_PROVIDER_RESPONSE);
	}

	@Test
	void deterministicFixturesCoverTheDocumentedScenariosAndDefaultToSuccess() {
		DeterministicDigitalCredentialListingAdapter adapter = new DeterministicDigitalCredentialListingAdapter();

		assertThat(adapter.listDigitalCredentials("00000020", CORRELATION_ID).digitalCredentials()).isEmpty();
		assertThat(adapter.listDigitalCredentials("00000021", CORRELATION_ID).digitalCredentials()).hasSize(1);
		assertThat(adapter.listDigitalCredentials("00000022", CORRELATION_ID).digitalCredentials()).hasSize(3);
		assertThat(adapter.listDigitalCredentials("00000023", CORRELATION_ID).digitalCredentials())
				.extracting(DigitalCredentialListingResult.ListedDigitalCredential::digitalCredentialUuid)
				.containsOnly("11111111-1111-4111-8111-111111111111");
		assertThat(adapter.listDigitalCredentials("87654321", CORRELATION_ID).digitalCredentials()).hasSize(3);
		assertThat(adapter.listDigitalCredentials("87654321", CORRELATION_ID).digitalCredentials())
				.extracting(DigitalCredentialListingResult.ListedDigitalCredential::status)
				.containsExactly(DigitalCredentialStatus.ACTIVE, DigitalCredentialStatus.ACTIVE,
						DigitalCredentialStatus.REVOKED);
		assertThat(adapter.listDigitalCredentials("00000025", CORRELATION_ID).outcome())
				.isEqualTo(DigitalCredentialListingResult.Outcome.TIMEOUT);
		assertThat(adapter.listDigitalCredentials("00000026", CORRELATION_ID).outcome())
				.isEqualTo(DigitalCredentialListingResult.Outcome.UNAVAILABLE);
		assertThat(adapter.listDigitalCredentials("00000027", CORRELATION_ID).outcome())
				.isEqualTo(DigitalCredentialListingResult.Outcome.MALFORMED);
	}

	@Test
	void refreshesAnExistingSnapshotAndExposesTheAuthoritativeRevocationDate() {
		DigitalCredentialListingPersistenceCoordinator persistence = mock(DigitalCredentialListingPersistenceCoordinator.class);
		RevocationRequestDigitalCredentialEntity active = persisted(31,
				"11111111-1111-4111-8111-111111111111", DigitalCredentialAvailabilityStatus.AVAILABLE, null);
		Instant revokedAt = Instant.parse("2025-03-01T12:00:00Z");
		RevocationRequestDigitalCredentialEntity revoked = persisted(32,
				"22222222-2222-4222-8222-222222222222", DigitalCredentialAvailabilityStatus.REVOKED, revokedAt);
		when(persistence.prepare(eq(REQUEST_ID), eq(CORRELATION_ID), any()))
				.thenReturn(new DigitalCredentialListingPersistenceCoordinator.Preparation(
						REQUEST_ID, "12345678", List.of(active, revoked),
						RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE,
						RevocationRequestStatus.DIGITAL_CREDENTIALS_AVAILABLE, true));
		when(persistence.complete(eq(REQUEST_ID), any(), eq(CORRELATION_ID)))
				.thenReturn(List.of(active, revoked));

		DigitalCredentialListResponse response = new DigitalCredentialListingService(
				(dni, correlation) -> success(List.of(
						listed(31, "11111111-1111-4111-8111-111111111111"),
						new DigitalCredentialListingResult.ListedDigitalCredential(32,
								"DniPeruanoCredential", Instant.parse("2025-01-01T10:00:00Z"),
								"22222222-2222-4222-8222-222222222222",
								DigitalCredentialStatus.REVOKED, revokedAt, 1))),
				properties(), persistence).list(REQUEST_ID, CORRELATION_ID);

		assertThat(response.canContinue()).isTrue();
		assertThat(response.digitalCredentials()).extracting(DigitalCredentialListResponse.DigitalCredentialItem::status)
				.containsExactly(DigitalCredentialStatus.ACTIVE, DigitalCredentialStatus.REVOKED);
		assertThat(response.digitalCredentials().get(0).revokedAt()).isNull();
		assertThat(response.digitalCredentials().get(1).revokedAt()).isEqualTo(revokedAt);
	}

	@Test
	void rejectsMissingOrInconsistentRevocationMetadata() {
		assertInvalid(List.of(new DigitalCredentialListingResult.ListedDigitalCredential(
				31, "DniPeruanoCredential", Instant.parse("2025-01-01T10:00:00Z"),
				"11111111-1111-4111-8111-111111111111", DigitalCredentialStatus.REVOKED, null, 1)));
		assertInvalid(List.of(new DigitalCredentialListingResult.ListedDigitalCredential(
				31, "DniPeruanoCredential", Instant.parse("2025-01-01T10:00:00Z"),
				"11111111-1111-4111-8111-111111111111", DigitalCredentialStatus.ACTIVE,
				Instant.parse("2025-02-01T10:00:00Z"), 0)));
	}

	@Test
	void acceptsAListWithoutAnActiveDigitalCredentialForPersistence() {
		DigitalCredentialListingPersistenceCoordinator persistence = preparedCoordinator();
		DigitalCredentialListingPort provider = (dni, correlation) -> success(List.of(
				new DigitalCredentialListingResult.ListedDigitalCredential(
				31, "DniPeruanoCredential", Instant.parse("2025-01-01T10:00:00Z"),
				"11111111-1111-4111-8111-111111111111", DigitalCredentialStatus.REVOKED,
				Instant.parse("2025-02-01T10:00:00Z"), 1)));
		when(persistence.complete(eq(REQUEST_ID), any(), eq(CORRELATION_ID))).thenReturn(List.of());
		new DigitalCredentialListingService(provider, properties(), persistence).list(REQUEST_ID, CORRELATION_ID);
		verify(persistence).complete(eq(REQUEST_ID), any(), eq(CORRELATION_ID));
	}

	@Test
	void revalidatesTheSelectedTupleAgainstAFreshProviderList() {
		DigitalCredentialListingPersistenceCoordinator persistence = preparedCoordinator();
		DigitalCredentialListingPort provider = (dni, correlation) -> success(List.of(
				listed(31, "11111111-1111-4111-8111-111111111111")));
		when(persistence.completeForConfirmation(eq(REQUEST_ID), any(),
				eq("11111111-1111-4111-8111-111111111111"), eq(31), eq(CORRELATION_ID)))
				.thenReturn(new DigitalCredentialListingPersistenceCoordinator.RevalidationCompletion(
						List.of(), true));

		DigitalCredentialListingService.RevalidationOutcome outcome =
				new DigitalCredentialListingService(provider, properties(), persistence)
						.revalidateSelection(REQUEST_ID,
								"11111111-1111-4111-8111-111111111111", 31, CORRELATION_ID);

		assertThat(outcome).isEqualTo(DigitalCredentialListingService.RevalidationOutcome.CURRENT);
		verify(persistence).completeForConfirmation(eq(REQUEST_ID), any(),
				eq("11111111-1111-4111-8111-111111111111"), eq(31), eq(CORRELATION_ID));
	}

	@Test
	void reportsASelectionThatIsNoLongerActiveAfterRevalidation() {
		DigitalCredentialListingPersistenceCoordinator persistence = preparedCoordinator();
		DigitalCredentialListingPort provider = (dni, correlation) -> success(List.of());
		when(persistence.completeForConfirmation(eq(REQUEST_ID), any(),
				eq("11111111-1111-4111-8111-111111111111"), eq(31), eq(CORRELATION_ID)))
				.thenReturn(new DigitalCredentialListingPersistenceCoordinator.RevalidationCompletion(
						List.of(), false));

		DigitalCredentialListingService.RevalidationOutcome outcome =
				new DigitalCredentialListingService(provider, properties(), persistence)
						.revalidateSelection(REQUEST_ID,
								"11111111-1111-4111-8111-111111111111", 31, CORRELATION_ID);

		assertThat(outcome).isEqualTo(DigitalCredentialListingService.RevalidationOutcome.STALE);
	}

	private void assertProviderFailure(DigitalCredentialListingResult.Outcome outcome,
			DigitalCredentialListingException.Reason expected) {
		DigitalCredentialListingPersistenceCoordinator persistence = preparedCoordinator();
		DigitalCredentialListingPort provider = (dni, correlation) ->
				new DigitalCredentialListingResult(outcome, List.of(), null, "MOCK_FAILURE");

		assertThatThrownBy(() -> new DigitalCredentialListingService(provider, properties(), persistence)
				.list(REQUEST_ID, CORRELATION_ID))
				.isInstanceOf(DigitalCredentialListingException.class)
				.extracting(error -> ((DigitalCredentialListingException) error).reason()).isEqualTo(expected);
		verify(persistence).restoreAfterFailure(REQUEST_ID, RevocationRequestStatus.IDENTITY_VERIFIED,
				expected.name(), CORRELATION_ID);
	}

	private static DigitalCredentialListingPersistenceCoordinator preparedCoordinator() {
		DigitalCredentialListingPersistenceCoordinator persistence = mock(DigitalCredentialListingPersistenceCoordinator.class);
		when(persistence.prepare(eq(REQUEST_ID), eq(CORRELATION_ID), any()))
				.thenReturn(new DigitalCredentialListingPersistenceCoordinator.Preparation(
						REQUEST_ID, "12345678", List.of(),
						RevocationRequestStatus.IDENTITY_VERIFIED,
						RevocationRequestStatus.IDENTITY_VERIFIED,
						true));
		return persistence;
	}

	private static DigitalCredentialListingProperties properties() {
		return new DigitalCredentialListingProperties();
	}

	private static DigitalCredentialListingResult success(
			List<DigitalCredentialListingResult.ListedDigitalCredential> digitalCredentials) {
		return new DigitalCredentialListingResult(DigitalCredentialListingResult.Outcome.SUCCESS,
				digitalCredentials, "mock-reference", null);
	}

	private static DigitalCredentialListingResult.ListedDigitalCredential listed(int index, String uuid) {
		return new DigitalCredentialListingResult.ListedDigitalCredential(
				index, "DniPeruanoCredential", Instant.parse("2025-01-01T10:00:00Z"), uuid,
				DigitalCredentialStatus.ACTIVE, null, 0);
	}

	private void assertInvalid(List<DigitalCredentialListingResult.ListedDigitalCredential> digitalCredentials) {
		DigitalCredentialListingPersistenceCoordinator persistence = preparedCoordinator();
		DigitalCredentialListingPort provider = (dni, correlation) -> success(digitalCredentials);
		assertThatThrownBy(() -> new DigitalCredentialListingService(provider, properties(), persistence)
				.list(REQUEST_ID, CORRELATION_ID))
				.isInstanceOf(DigitalCredentialListingException.class)
				.extracting(error -> ((DigitalCredentialListingException) error).reason())
				.isEqualTo(DigitalCredentialListingException.Reason.INVALID_PROVIDER_RESPONSE);
		verify(persistence).restoreAfterFailure(REQUEST_ID, RevocationRequestStatus.IDENTITY_VERIFIED,
				"INVALID_PROVIDER_RESPONSE", CORRELATION_ID);
	}

	private static RevocationRequestDigitalCredentialEntity persisted(int index, String uuid,
			DigitalCredentialAvailabilityStatus status, Instant revokedAt) {
		RevocationRequestDigitalCredentialEntity entity = mock(RevocationRequestDigitalCredentialEntity.class);
		when(entity.getStatusListIndex()).thenReturn(index);
		when(entity.getEmissionCreatedAt()).thenReturn(Instant.parse("2025-01-01T10:00:00Z"));
		when(entity.getDigitalCredentialUuid()).thenReturn(uuid);
		when(entity.getAvailabilityStatus()).thenReturn(status);
		when(entity.getRevokedAt()).thenReturn(revokedAt);
		when(entity.isSelected()).thenReturn(false);
		return entity;
	}
}
