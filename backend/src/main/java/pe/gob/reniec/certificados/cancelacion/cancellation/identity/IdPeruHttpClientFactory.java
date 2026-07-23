package pe.gob.reniec.certificados.cancelacion.cancellation.identity;

import java.time.Duration;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class IdPeruHttpClientFactory {
	private final Duration connectTimeout;
	private final Duration readTimeout;

	public IdPeruHttpClientFactory() {
		this(IdPeruFlowSettings.CONNECT_TIMEOUT, IdPeruFlowSettings.READ_TIMEOUT);
	}

	IdPeruHttpClientFactory(Duration connectTimeout, Duration readTimeout) {
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
	}

	public RestClient create() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeout);
		factory.setReadTimeout(readTimeout);
		return RestClient.builder().requestFactory(factory).build();
	}
}
