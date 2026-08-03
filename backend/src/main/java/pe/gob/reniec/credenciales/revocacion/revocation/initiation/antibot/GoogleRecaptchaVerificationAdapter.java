package pe.gob.reniec.credenciales.revocacion.revocation.initiation.antibot;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

final class GoogleRecaptchaVerificationAdapter implements AntiBotVerificationPort {

	private static final int MAX_TOKEN_LENGTH = 4096;
	private final GoogleRecaptchaProperties properties;
	private final RestClient client;

	GoogleRecaptchaVerificationAdapter(GoogleRecaptchaProperties properties) {
		this.properties = properties;
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.timeout());
		requestFactory.setReadTimeout(properties.timeout());
		this.client = RestClient.builder().requestFactory(requestFactory).build();
	}

	@Override
	public void verify(String token) {
		if (token == null || token.isBlank()) throw failure(RecaptchaFailure.REQUIRED);
		if (token.length() > MAX_TOKEN_LENGTH) throw failure(RecaptchaFailure.REJECTED);

		LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("secret", properties.secretKey());
		form.add("response", token);

		GoogleSiteVerifyResponse response;
		try {
			response = client.post()
					.uri(properties.verifyUri())
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(GoogleSiteVerifyResponse.class);
		}
		catch (ResourceAccessException exception) {
			if (hasTimeoutCause(exception)) throw new RecaptchaVerificationException(RecaptchaFailure.TIMEOUT, exception);
			throw new RecaptchaVerificationException(RecaptchaFailure.UNAVAILABLE, exception);
		}
		catch (HttpStatusCodeException exception) {
			throw new RecaptchaVerificationException(RecaptchaFailure.UNAVAILABLE, exception);
		}
		catch (RestClientException exception) {
			if (hasTimeoutCause(exception)) throw new RecaptchaVerificationException(RecaptchaFailure.TIMEOUT, exception);
			throw new RecaptchaVerificationException(RecaptchaFailure.INVALID_RESPONSE, exception);
		}

		validate(response);
	}

	private void validate(GoogleSiteVerifyResponse response) {
		if (response == null || response.success() == null) throw failure(RecaptchaFailure.INVALID_RESPONSE);
		if (!response.success()) {
			if (response.errorCodes() != null && response.errorCodes().contains("timeout-or-duplicate")) {
				throw failure(RecaptchaFailure.EXPIRED_OR_DUPLICATE);
			}
			throw failure(RecaptchaFailure.REJECTED);
		}

		String hostname = response.hostname();
		if (hostname != null && !hostname.isBlank()
				&& !properties.allowedHostnames().contains(hostname.toLowerCase(Locale.ROOT))) {
			throw failure(RecaptchaFailure.REJECTED);
		}
	}

	private static boolean hasTimeoutCause(Throwable throwable) {
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			if (current instanceof SocketTimeoutException
					|| current instanceof java.net.http.HttpTimeoutException) return true;
		}
		return false;
	}

	private static RecaptchaVerificationException failure(RecaptchaFailure failure) {
		return new RecaptchaVerificationException(failure);
	}

	private record GoogleSiteVerifyResponse(Boolean success, String hostname,
			@JsonProperty("error-codes") List<String> errorCodes) {
	}
}
