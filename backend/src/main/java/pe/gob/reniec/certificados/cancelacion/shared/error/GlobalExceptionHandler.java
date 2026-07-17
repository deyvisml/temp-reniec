package pe.gob.reniec.certificados.cancelacion.shared.error;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import pe.gob.reniec.certificados.cancelacion.shared.web.CorrelationIdFilter;
import pe.gob.reniec.certificados.cancelacion.cancellation.eligibility.EligibilityConcurrencyException;
import pe.gob.reniec.certificados.cancelacion.cancellation.eligibility.EligibilityInProgressException;
import pe.gob.reniec.certificados.cancelacion.cancellation.eligibility.EligibilityProviderException;
import pe.gob.reniec.certificados.cancelacion.cancellation.eligibility.EligibilityTimeoutException;
import pe.gob.reniec.certificados.cancelacion.cancellation.eligibility.EligibilityUnavailableException;
import pe.gob.reniec.certificados.cancelacion.system.DependencyUnavailableException;

@RestControllerAdvice
public final class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler({ MethodArgumentNotValidException.class, HandlerMethodValidationException.class })
	ResponseEntity<ApiError> handleValidation(Exception exception, HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La solicitud contiene datos inválidos.", request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> handleMalformedRequest(HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return respond(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "La solicitud no tiene un formato válido.",
				request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception,
			HttpServletRequest request) {
		return respond(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
				"El método HTTP no está permitido para esta ruta.", request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException exception,
			HttpServletRequest request) {
		return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
				"El tipo de contenido no está permitido.", request);
	}

	@ExceptionHandler(EligibilityInProgressException.class)
	ResponseEntity<ApiError> handleEligibilityInProgress(EligibilityInProgressException exception,
			HttpServletRequest request) {
		return respond(HttpStatus.CONFLICT, "ELIGIBILITY_IN_PROGRESS",
				"La consulta ya se está procesando. Inténtalo nuevamente en unos segundos.", request);
	}

	@ExceptionHandler(EligibilityConcurrencyException.class)
	ResponseEntity<ApiError> handleEligibilityConcurrency(EligibilityConcurrencyException exception,
			HttpServletRequest request) {
		return respond(HttpStatus.CONFLICT, "CONCURRENT_REQUEST",
				"La solicitud fue actualizada simultáneamente. Inténtalo nuevamente.", request);
	}

	@ExceptionHandler(EligibilityUnavailableException.class)
	ResponseEntity<ApiError> handleEligibilityUnavailable(EligibilityUnavailableException exception,
			HttpServletRequest request) {
		return respond(HttpStatus.SERVICE_UNAVAILABLE, "ELIGIBILITY_UNAVAILABLE",
				"No podemos consultar los certificados en este momento. Inténtalo más tarde.", request);
	}

	@ExceptionHandler(EligibilityTimeoutException.class)
	ResponseEntity<ApiError> handleEligibilityTimeout(EligibilityTimeoutException exception,
			HttpServletRequest request) {
		return respond(HttpStatus.GATEWAY_TIMEOUT, "ELIGIBILITY_TIMEOUT",
				"La consulta tardó demasiado. Inténtalo nuevamente.", request);
	}

	@ExceptionHandler(EligibilityProviderException.class)
	ResponseEntity<ApiError> handleEligibilityProvider(EligibilityProviderException exception,
			HttpServletRequest request) {
		return respond(HttpStatus.BAD_GATEWAY, "ELIGIBILITY_PROVIDER_ERROR",
				"No fue posible completar la consulta. Inténtalo nuevamente.", request);
	}

	@ExceptionHandler({ NoHandlerFoundException.class, NoResourceFoundException.class })
	ResponseEntity<ApiError> handleNotFound(Exception exception, HttpServletRequest request) {
		return respond(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "No se encontró el recurso solicitado.", request);
	}

	@ExceptionHandler(DependencyUnavailableException.class)
	ResponseEntity<ApiError> handleDependencyUnavailable(DependencyUnavailableException exception,
			HttpServletRequest request) {
		LOGGER.warn("Technical dependency unavailable path={}", request.getRequestURI());
		return respond(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE",
				"El servicio no está disponible temporalmente.", request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
		LOGGER.error("Unhandled request failure type={} path={}", exception.getClass().getSimpleName(),
				request.getRequestURI());
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
				"No fue posible procesar la solicitud.", request);
	}

	private ResponseEntity<ApiError> respond(HttpStatus status, String code, String message,
			HttpServletRequest request) {
		String path = request.getRequestURI();
		String correlationId = String.valueOf(request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
		LOGGER.warn("Request rejected code={} path={}", code, path);

		ApiError error = new ApiError(code, message, Instant.now(), path, correlationId);
		return ResponseEntity.status(status).body(error);
	}
}
