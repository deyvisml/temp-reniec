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
import pe.gob.reniec.certificados.cancelacion.cancellation.initiation.CancellationRequestProtectedException;
import pe.gob.reniec.certificados.cancelacion.cancellation.initiation.CancellationRequestConcurrencyException;
import pe.gob.reniec.certificados.cancelacion.cancellation.initiation.AvailabilityCheckInProgressException;
import pe.gob.reniec.certificados.cancelacion.cancellation.initiation.AvailabilityProviderException;
import pe.gob.reniec.certificados.cancelacion.cancellation.initiation.AvailabilityTimeoutException;
import pe.gob.reniec.certificados.cancelacion.cancellation.initiation.AvailabilityUnavailableException;
import pe.gob.reniec.certificados.cancelacion.system.DependencyUnavailableException;
import pe.gob.reniec.certificados.cancelacion.cancellation.initiation.antibot.RecaptchaFailure;
import pe.gob.reniec.certificados.cancelacion.cancellation.initiation.antibot.RecaptchaVerificationException;

@RestControllerAdvice
public final class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler({ MethodArgumentNotValidException.class, HandlerMethodValidationException.class })
	ResponseEntity<ApiError> handleValidation(Exception exception, HttpServletRequest request) {
		if (exception instanceof MethodArgumentNotValidException validation
				&& validation.getBindingResult().getFieldErrors("recaptchaToken").stream()
						.anyMatch(error -> error.getRejectedValue() == null
								|| String.valueOf(error.getRejectedValue()).isBlank())) {
			return respond(HttpStatus.BAD_REQUEST, "RECAPTCHA_REQUIRED",
					"Completa la verificación de seguridad para continuar.", request);
		}
		return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La solicitud contiene datos inválidos.", request);
	}

	@ExceptionHandler(RecaptchaVerificationException.class)
	ResponseEntity<ApiError> handleRecaptcha(RecaptchaVerificationException exception, HttpServletRequest request) {
		RecaptchaFailure failure = exception.failure();
		return switch (failure) {
			case REQUIRED -> respond(HttpStatus.BAD_REQUEST, "RECAPTCHA_REQUIRED",
					"Completa la verificación de seguridad para continuar.", request);
			case REJECTED -> respond(HttpStatus.BAD_REQUEST, "RECAPTCHA_REJECTED",
					"No pudimos validar la verificación de seguridad. Complétala nuevamente.", request);
			case EXPIRED_OR_DUPLICATE -> respond(HttpStatus.BAD_REQUEST, "RECAPTCHA_EXPIRED_OR_DUPLICATE",
					"La verificación de seguridad expiró o ya fue utilizada. Complétala nuevamente.", request);
			case UNAVAILABLE -> respond(HttpStatus.SERVICE_UNAVAILABLE, "RECAPTCHA_UNAVAILABLE",
					"La verificación de seguridad no está disponible temporalmente.", request);
			case TIMEOUT -> respond(HttpStatus.GATEWAY_TIMEOUT, "RECAPTCHA_TIMEOUT",
					"La verificación de seguridad tardó demasiado. Inténtalo nuevamente.", request);
			case INVALID_RESPONSE -> respond(HttpStatus.BAD_GATEWAY, "RECAPTCHA_INVALID_RESPONSE",
					"No fue posible confirmar la verificación de seguridad. Inténtalo nuevamente.", request);
		};
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

	@ExceptionHandler(AvailabilityCheckInProgressException.class)
	ResponseEntity<ApiError> handleAvailabilityCheckInProgress(AvailabilityCheckInProgressException exception,
			HttpServletRequest request) {
		return respond(HttpStatus.CONFLICT, "AVAILABILITY_CHECK_IN_PROGRESS",
				"La consulta ya se está procesando. Inténtalo nuevamente en unos segundos.", request);
	}

	@ExceptionHandler(CancellationRequestConcurrencyException.class)
	ResponseEntity<ApiError> handleCancellationRequestConcurrency(CancellationRequestConcurrencyException exception,
			HttpServletRequest request) {
		return respond(HttpStatus.CONFLICT, "CONCURRENT_REQUEST",
				"La solicitud fue actualizada simultáneamente. Inténtalo nuevamente.", request);
	}

	@ExceptionHandler(CancellationRequestProtectedException.class)
	ResponseEntity<ApiError> handleProtectedCancellationRequest(CancellationRequestProtectedException exception,
			HttpServletRequest request) {
		return respond(HttpStatus.CONFLICT, "CANCELLATION_REQUEST_IN_PROGRESS",
				"No es posible iniciar una nueva solicitud en este momento. Inténtalo nuevamente más adelante.", request);
	}

	@ExceptionHandler(AvailabilityUnavailableException.class)
	ResponseEntity<ApiError> handleAvailabilityUnavailable(AvailabilityUnavailableException exception,
			HttpServletRequest request) {
		return respond(HttpStatus.SERVICE_UNAVAILABLE, "AVAILABILITY_UNAVAILABLE",
				"No podemos consultar los certificados en este momento. Inténtalo más tarde.", request);
	}

	@ExceptionHandler(AvailabilityTimeoutException.class)
	ResponseEntity<ApiError> handleAvailabilityTimeout(AvailabilityTimeoutException exception,
			HttpServletRequest request) {
		return respond(HttpStatus.GATEWAY_TIMEOUT, "AVAILABILITY_TIMEOUT",
				"La consulta tardó demasiado. Inténtalo nuevamente.", request);
	}

	@ExceptionHandler(AvailabilityProviderException.class)
	ResponseEntity<ApiError> handleAvailabilityProvider(AvailabilityProviderException exception,
			HttpServletRequest request) {
		return respond(HttpStatus.BAD_GATEWAY, "AVAILABILITY_PROVIDER_ERROR",
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
