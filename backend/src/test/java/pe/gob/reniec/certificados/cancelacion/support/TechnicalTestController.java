package pe.gob.reniec.certificados.cancelacion.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class TechnicalTestController {

	@PostMapping("/__test/validation")
	void validate(@Valid @RequestBody ValidationRequest request) {
	}

	@GetMapping("/__test/failure")
	void fail() {
		throw new IllegalStateException("sensitive-internal-message");
	}

	record ValidationRequest(@NotBlank String value) {
	}
}
