package pe.gob.reniec.credenciales.revocacion.revocation.initiation;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import pe.gob.reniec.credenciales.revocacion.revocation.initiation.AvailabilityPersistenceCoordinator.AvailabilityPreparation;
import pe.gob.reniec.credenciales.revocacion.revocation.initiation.antibot.AntiBotVerificationPort;

@Service
public class RevocationRequestInitiationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(RevocationRequestInitiationService.class);
	private static final String OPERATION = "has-credentials";

	private final AvailabilityPersistenceCoordinator persistence;
	private final DigitalCredentialAvailabilityPort availabilityPort;
	private final AntiBotVerificationPort antiBotVerificationPort;
	private final Duration timeout;
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	public RevocationRequestInitiationService(AvailabilityPersistenceCoordinator persistence,
			DigitalCredentialAvailabilityPort availabilityPort,
			AntiBotVerificationPort antiBotVerificationPort,
			AvailabilityProperties properties) {
		this.persistence = persistence;
		this.availabilityPort = availabilityPort;
		this.antiBotVerificationPort = antiBotVerificationPort;
		this.timeout = properties.getTimeout();
	}

	public RevocationRequestResponse initiate(String dni, String recaptchaToken, String correlationId) {
		try {
			antiBotVerificationPort.verify(recaptchaToken);
			AvailabilityPreparation preparation = persistence.prepare(dni, correlationId);
			AvailabilityResult result = execute(dni, preparation);
			RevocationRequestResponse response = persistence.finalizeAttempt(preparation, result);
			if (result.outcome() == AvailabilityOutcome.UNAVAILABLE
					&& "PROVIDER_TIMEOUT".equals(result.technicalCode())) throw new AvailabilityTimeoutException();
			if (result.outcome() == AvailabilityOutcome.UNAVAILABLE) throw new AvailabilityUnavailableException();
			if (result.outcome() == AvailabilityOutcome.ERROR) throw new AvailabilityProviderException();
			return response;
		}
		catch (RevocationRequestProtectedException | AvailabilityCheckInProgressException | AvailabilityUnavailableException
				| AvailabilityTimeoutException | AvailabilityProviderException exception) {
			throw exception;
		}
		catch (ConcurrencyFailureException | DataIntegrityViolationException exception) {
			throw new RevocationRequestConcurrencyException(exception);
		}
	}

	private AvailabilityResult execute(String dni, AvailabilityPreparation preparation) {
		long startedAt = System.nanoTime();
		Future<AvailabilityResult> future = executor.submit(() -> availabilityPort.checkAvailability(dni));
		try {
			AvailabilityResult result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
			if (result == null) {
				logFailure(startedAt, "ERROR", "AVAILABILITY_INVALID_RESPONSE");
				failAttempt(preparation, AvailabilityOutcome.ERROR, "AVAILABILITY_INVALID_RESPONSE");
				throw new AvailabilityProviderException();
			}
			LOGGER.info("Credential provider operation={} outcome={} durationMs={} technicalCode={}",
					OPERATION, result.outcome(), elapsedMillis(startedAt), result.technicalCode());
			return result;
		}
		catch (TimeoutException exception) {
			future.cancel(true);
			logFailure(startedAt, "TIMEOUT", "AVAILABILITY_TIMEOUT");
			failAttempt(preparation, AvailabilityOutcome.UNAVAILABLE, "AVAILABILITY_TIMEOUT");
			throw new AvailabilityTimeoutException();
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			logFailure(startedAt, "ERROR", "AVAILABILITY_INTERRUPTED");
			failAttempt(preparation, AvailabilityOutcome.ERROR, "AVAILABILITY_INTERRUPTED");
			throw new AvailabilityProviderException();
		}
		catch (ExecutionException exception) {
			logFailure(startedAt, "ERROR", "AVAILABILITY_PROVIDER_ERROR");
			failAttempt(preparation, AvailabilityOutcome.ERROR, "AVAILABILITY_PROVIDER_ERROR");
			throw new AvailabilityProviderException();
		}
	}

	private void failAttempt(AvailabilityPreparation preparation, AvailabilityOutcome outcome, String technicalCode) {
		persistence.finalizeAttempt(preparation,
				new AvailabilityResult(outcome, null, technicalCode));
	}

	private static void logFailure(long startedAt, String outcome, String technicalCode) {
		LOGGER.warn("Credential provider operation={} outcome={} durationMs={} technicalCode={}",
				OPERATION, outcome, elapsedMillis(startedAt), technicalCode);
	}

	private static long elapsedMillis(long startedAt) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
	}

	@PreDestroy
	void closeExecutor() {
		executor.close();
	}
}
