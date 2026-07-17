package pe.gob.reniec.certificados.cancelacion.cancellation.eligibility;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import pe.gob.reniec.certificados.cancelacion.cancellation.eligibility.EligibilityPersistenceCoordinator.EligibilityPreparation;

@Service
public class EligibilityInitiationService {

	private final EligibilityPersistenceCoordinator persistence;
	private final CertificateEligibilityGateway gateway;
	private final Duration timeout;
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	public EligibilityInitiationService(EligibilityPersistenceCoordinator persistence,
			CertificateEligibilityGateway gateway,
			@Value("${app.eligibility.timeout:1s}") Duration timeout) {
		this.persistence = persistence;
		this.gateway = gateway;
		this.timeout = timeout;
	}

	public CancellationRequestResponse initiate(String dni, String correlationId) {
		try {
			EligibilityPreparation preparation = persistence.prepare(dni, correlationId);
			if (preparation.recovered()) return preparation.recoveredResponse();

			EligibilityGatewayResult result = execute(dni, preparation);
			CancellationRequestResponse response = persistence.finalizeAttempt(preparation, result);
			if (result.outcome() == EligibilityOutcome.UNAVAILABLE) throw new EligibilityUnavailableException();
			if (result.outcome() == EligibilityOutcome.ERROR) throw new EligibilityProviderException();
			return response;
		}
		catch (EligibilityInProgressException | EligibilityUnavailableException
				| EligibilityTimeoutException | EligibilityProviderException exception) {
			throw exception;
		}
		catch (ConcurrencyFailureException | DataIntegrityViolationException exception) {
			throw new EligibilityConcurrencyException(exception);
		}
	}

	private EligibilityGatewayResult execute(String dni, EligibilityPreparation preparation) {
		Future<EligibilityGatewayResult> future = executor.submit(() -> gateway.check(dni));
		try {
			return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException exception) {
			future.cancel(true);
			persistence.finalizeAttempt(preparation,
					new EligibilityGatewayResult(EligibilityOutcome.UNAVAILABLE, null, "ELIGIBILITY_TIMEOUT"));
			throw new EligibilityTimeoutException();
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			persistence.finalizeAttempt(preparation,
					new EligibilityGatewayResult(EligibilityOutcome.ERROR, null, "ELIGIBILITY_INTERRUPTED"));
			throw new EligibilityProviderException();
		}
		catch (ExecutionException exception) {
			persistence.finalizeAttempt(preparation,
					new EligibilityGatewayResult(EligibilityOutcome.ERROR, null, "ELIGIBILITY_PROVIDER_ERROR"));
			throw new EligibilityProviderException();
		}
	}

	@PreDestroy
	void closeExecutor() {
		executor.close();
	}
}
