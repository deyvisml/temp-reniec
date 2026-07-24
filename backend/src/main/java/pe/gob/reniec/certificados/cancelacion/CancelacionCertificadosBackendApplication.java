package pe.gob.reniec.certificados.cancelacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CancelacionCertificadosBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CancelacionCertificadosBackendApplication.class, args);
	}

}
