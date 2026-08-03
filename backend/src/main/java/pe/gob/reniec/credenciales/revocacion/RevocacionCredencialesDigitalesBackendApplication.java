package pe.gob.reniec.credenciales.revocacion;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class RevocacionCredencialesDigitalesBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(RevocacionCredencialesDigitalesBackendApplication.class, args);
	}

	@Bean
	Clock applicationClock() {
		return Clock.systemUTC();
	}

}
