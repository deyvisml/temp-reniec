package pe.gob.reniec.credenciales.revocacion.revocation.initiation;

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

import pe.gob.reniec.credenciales.revocacion.revocation.initiation.AvailabilityPersistenceCoordinator.AvailabilityPreparation;
import pe.gob.reniec.credenciales.revocacion.revocation.initiation.antibot.AntiBotVerificationPort;

@Service
public class RevocationRequestInitiationService {

	private final AvailabilityPersistenceCoordinator persistence;
	private final DigitalCredentialAvailabilityPort availabilityPort;
	private final AntiBotVerificationPort antiBotVerificationPort;
	private final Duration timeout;
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	public RevocationRequestInitiationService(AvailabilityPersistenceCoordinator persistence,
			DigitalCredentialAvailabilityPort availabilityPort,
			AntiBotVerificationPort antiBotVerificationPort,
			@Value("${app.availability.timeout:1s}") Duration timeout) {
		this.persistence = persistence;
		this.availabilityPort = availabilityPort;
		this.antiBotVerificationPort = antiBotVerificationPort;
		this.timeout = requirePositive(timeout, "app.availability.timeout");
	}

	public RevocationRequestResponse initiate(String dni, String recaptchaToken, String correlationId) {
		try {
			antiBotVerificationPort.verify(recaptchaToken);
			AvailabilityPreparation preparation = persistence.prepare(dni, correlationId);
			AvailabilityResult result = execute(dni, preparation);
			RevocationRequestResponse response = persistence.finalizeAttempt(preparation, result);
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
		Future<AvailabilityResult> future = executor.submit(() -> availabilityPort.checkAvailability(dni));
		try {
			AvailabilityResult result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
			if (result == null) {
				failAttempt(preparation, AvailabilityOutcome.ERROR, "AVAILABILITY_INVALID_RESPONSE");
				throw new AvailabilityProviderException();
			}
			return result;
		}
		catch (TimeoutException exception) {
			future.cancel(true);
			failAttempt(preparation, AvailabilityOutcome.UNAVAILABLE, "AVAILABILITY_TIMEOUT");
			throw new AvailabilityTimeoutException();
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			failAttempt(preparation, AvailabilityOutcome.ERROR, "AVAILABILITY_INTERRUPTED");
			throw new AvailabilityProviderException();
		}
		catch (ExecutionException exception) {
			failAttempt(preparation, AvailabilityOutcome.ERROR, "AVAILABILITY_PROVIDER_ERROR");
			throw new AvailabilityProviderException();
		}
	}

	private void failAttempt(AvailabilityPreparation preparation, AvailabilityOutcome outcome, String technicalCode) {
		persistence.finalizeAttempt(preparation,
				new AvailabilityResult(outcome, null, technicalCode));
	}

	private static Duration requirePositive(Duration value, String property) {
		if (value == null || value.isZero() || value.isNegative()) {
			throw new IllegalArgumentException(property + " must be greater than zero");
		}
		return value;
	}

	@PreDestroy
	void closeExecutor() {
		executor.close();
	}
}
