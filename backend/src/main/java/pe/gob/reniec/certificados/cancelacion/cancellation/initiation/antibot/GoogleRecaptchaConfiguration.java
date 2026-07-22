package pe.gob.reniec.certificados.cancelacion.cancellation.initiation.antibot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.recaptcha.mode", havingValue = "google")
@EnableConfigurationProperties(GoogleRecaptchaProperties.class)
class GoogleRecaptchaConfiguration {

	@Bean
	AntiBotVerificationPort googleRecaptchaVerificationAdapter(GoogleRecaptchaProperties properties) {
		return new GoogleRecaptchaVerificationAdapter(properties);
	}
}
