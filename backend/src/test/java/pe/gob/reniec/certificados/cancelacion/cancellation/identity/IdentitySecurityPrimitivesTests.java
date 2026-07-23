package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class IdentitySecurityPrimitivesTests {
	private IdPeruProperties properties;

	@BeforeEach
	void setup() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("test");
		ApplicationUrlProperties urls = new ApplicationUrlProperties();
		urls.setBackendBaseUrl(java.net.URI.create("http://localhost:8080"));
		urls.setFrontendBaseUrl(java.net.URI.create("http://localhost:3000"));
		properties = new IdPeruProperties(environment, urls);
		properties.setMode(IdPeruMode.MOCK);
		properties.setFlowSecret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
		properties.validate();
	}

	@Test
	void createsUniqueSingleUseFriendlyStateAndRfc7636Pkce() {
		IdentitySecurityArtifacts artifacts = new IdentitySecurityArtifacts();
		Set<String> states = new HashSet<>();
		for (int i = 0; i < 100; i++) states.add(artifacts.newState().value());
		assertThat(states).hasSize(100).allMatch(value -> value.matches("[A-Za-z0-9_-]{43}"));
		IdentitySecurityArtifacts.PkceValue pkce = artifacts.newPkce();
		assertThat(pkce.verifier()).matches("[A-Za-z0-9_-]{64}");
		assertThat(pkce.challenge()).matches("[A-Za-z0-9_-]{43}");
	}

	@Test
	void protectsPkceWithAuthenticatedEncryptionAndRejectsTampering() {
		TransientSecretProtector protector = new TransientSecretProtector(new IdPeruFlowKeys(properties));
		String protectedValue = protector.protect("verifier-ficticio");
		assertThat(protectedValue).doesNotContain("verifier-ficticio");
		assertThat(protector.reveal(protectedValue)).isEqualTo("verifier-ficticio");
		assertThatThrownBy(() -> protector.reveal(protectedValue.substring(0, protectedValue.length() - 1) + "A"))
				.isInstanceOf(IdentityIntegrationException.class);
	}

	@Test
	void encryptsVdDeterministicallyAccordingToTheDocument() {
		String encrypted = new IdPeruDniEncryptor().encrypt("12345678", "1234567890abcdef-client");
		assertThat(encrypted).isEqualTo("+AT3s2G9zS0m3+9pw5TIIQ==");
	}

	@Test
	void signsPurposeBoundShortLivedContinuityWithoutPii() {
		FlowTokenService service = new FlowTokenService(properties, new IdPeruFlowKeys(properties));
		FlowTokenService.IssuedFlowToken token = service.issueIdentityInit(42L);
		assertThat(token.value()).doesNotContain("12345678");
		FlowTokenService.FlowTokenClaims claims = service.validate(token.value(), FlowTokenPurpose.IDENTITY_INIT);
		assertThat(claims.requestId()).isEqualTo(42L);
		assertThatThrownBy(() -> service.validate(token.value(), FlowTokenPurpose.FLOW_AUTH))
				.isInstanceOf(IdentityIntegrationException.class);
	}

	@Test
	void derivesStableAndPurposeSeparatedKeysFromOneMasterSecret() {
		IdPeruFlowKeys first = new IdPeruFlowKeys(properties);
		IdPeruFlowKeys second = new IdPeruFlowKeys(properties);
		assertThat(first.pkceEncryptionKey()).containsExactly(second.pkceEncryptionKey());
		assertThat(first.flowSigningKey()).containsExactly(second.flowSigningKey());
		assertThat(first.pkceEncryptionKey()).isNotEqualTo(first.flowSigningKey());
		assertThat(java.util.Base64.getEncoder().encodeToString(first.pkceEncryptionKey()))
				.isNotEqualTo(properties.getFlowSecret());
	}
}
