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

import pe.gob.reniec.certificados.cancelacion.cancellation.eligibility.EligibilityPersistenceCoordinator.AvailabilityPreparation;

@Service
public class EligibilityInitiationService {

	private final EligibilityPersistenceCoordinator persistence;
	private final CertificateAvailabilityPort availabilityPort;
	private final Duration timeout;
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	public EligibilityInitiationService(EligibilityPersistenceCoordinator persistence,
			CertificateAvailabilityPort availabilityPort,
			@Value("${app.availability.timeout:1s}") Duration timeout) {
		this.persistence = persistence;
		this.availabilityPort = availabilityPort;
		this.timeout = timeout;
	}

	public CancellationRequestResponse initiate(String dni, String correlationId) {
		try {
			AvailabilityPreparation preparation = persistence.prepare(dni, correlationId);
			AvailabilityResult result = execute(dni, preparation);
			CancellationRequestResponse response = persistence.finalizeAttempt(preparation, result);
			if (result.outcome() == AvailabilityOutcome.UNAVAILABLE) throw new EligibilityUnavailableException();
			if (result.outcome() == AvailabilityOutcome.ERROR) throw new EligibilityProviderException();
			return response;
		}
		catch (CancellationRequestProtectedException | EligibilityInProgressException | EligibilityUnavailableException
				| EligibilityTimeoutException | EligibilityProviderException exception) {
			throw exception;
		}
		catch (ConcurrencyFailureException | DataIntegrityViolationException exception) {
			throw new EligibilityConcurrencyException(exception);
		}
	}

	private AvailabilityResult execute(String dni, AvailabilityPreparation preparation) {
		Future<AvailabilityResult> future = executor.submit(() -> availabilityPort.checkAvailability(dni));
		try {
			return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException exception) {
			future.cancel(true);
			persistence.finalizeAttempt(preparation,
					new AvailabilityResult(AvailabilityOutcome.UNAVAILABLE, null, "AVAILABILITY_TIMEOUT"));
			throw new EligibilityTimeoutException();
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			persistence.finalizeAttempt(preparation,
					new AvailabilityResult(AvailabilityOutcome.ERROR, null, "AVAILABILITY_INTERRUPTED"));
			throw new EligibilityProviderException();
		}
		catch (ExecutionException exception) {
			persistence.finalizeAttempt(preparation,
					new AvailabilityResult(AvailabilityOutcome.ERROR, null, "AVAILABILITY_PROVIDER_ERROR"));
			throw new EligibilityProviderException();
		}
	}

	@PreDestroy
	void closeExecutor() {
		executor.close();
	}
}
