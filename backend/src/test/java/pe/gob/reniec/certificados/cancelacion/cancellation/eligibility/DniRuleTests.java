package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DniRuleTests {

	@Test
	void acceptsOnlyEightAsciiDigitsAndMasksOnlyTheLastTwo() {
		assertThat(DniRule.isValid("12345678")).isTrue();
		assertThat(DniRule.masked("12345678")).isEqualTo("******78");
		assertThat(DniRule.isValid(null)).isFalse();
		assertThat(DniRule.isValid("")).isFalse();
		assertThat(DniRule.isValid("1234567")).isFalse();
		assertThat(DniRule.isValid("123456789")).isFalse();
		assertThat(DniRule.isValid("1234 678")).isFalse();
		assertThat(DniRule.isValid("１２３４５６７８")).isFalse();
		assertThatThrownBy(() -> DniRule.masked("invalid")).isInstanceOf(IllegalArgumentException.class);
	}
}
