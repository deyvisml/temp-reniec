package pe.gob.reniec.credenciales.revocacion.revocation.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class MockCitizenIdentityProviderAdapterTests {

	@Test
	void successfulScenarioMatchesAnyValidDniAndUsesSyntheticName() {
		IdPeruProperties properties = properties();
		MockCitizenIdentityProviderAdapter adapter = new MockCitizenIdentityProviderAdapter(properties);

		for (String dni : new String[] { "00000000", "00000001", "99999999" }) {
			CitizenIdentityProviderPort.VerifiedCitizen citizen = adapter.authenticate(
					"mock-code", "mock-session", "unused-verifier", dni);

			assertThat(citizen.dni()).isEqualTo(dni);
			assertThat(citizen.firstName()).isEqualTo("PRUEBA");
		}
	}

	@Test
	void authorizationStaysInsideTheBackendAndCarriesOnlyState() {
		IdPeruProperties properties = properties();
		MockCitizenIdentityProviderAdapter adapter = new MockCitizenIdentityProviderAdapter(properties);

		URI authorization = adapter.authorizationUri(
				new CitizenIdentityProviderPort.AuthorizationContext("state-test", "challenge-test", "12345678"));

		assertThat(authorization).isEqualTo(URI.create(
				"http://localhost:8080/api/v1/identity-verifications/mock/authorize?state=state-test"));
		assertThat(authorization.toString()).doesNotContain("12345678", "challenge-test");
	}

	private static IdPeruProperties properties() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("local");
		ApplicationUrlProperties urls = new ApplicationUrlProperties();
		urls.setFrontendBaseUrl(URI.create("http://localhost:3000"));
		urls.setBackendBaseUrl(URI.create("http://localhost:8080"));
		IdPeruProperties properties = new IdPeruProperties(environment, urls);
		properties.setMode(IdPeruMode.MOCK);
		return properties;
	}
}
