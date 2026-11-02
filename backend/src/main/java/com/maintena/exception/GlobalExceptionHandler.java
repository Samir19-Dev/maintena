package com.maintena.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Traduce las excepciones de la aplicación a respuestas HTTP estándar
 * ({@link ProblemDetail}, RFC 9457).
 *
 * <p>Se conserva la propiedad {@code message} para no romper a los clientes
 * existentes y se añaden {@code timestamp}, {@code path} y {@code error}.</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ProblemDetail> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ProblemDetail> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(UnauthorizedActionException.class)
	public ResponseEntity<ProblemDetail> handleUnauthorized(UnauthorizedActionException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidTokenException.class)
	public ResponseEntity<ProblemDetail> handleInvalidToken(InvalidTokenException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}

	@ExceptionHandler(TooManyRequestsException.class)
	public ResponseEntity<ProblemDetail> handleTooManyRequests(TooManyRequestsException ex, HttpServletRequest request) {
		ProblemDetail problem = problem(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
		problem.setProperty("retryAfter", ex.getRetryAfterSeconds());
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
			.header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
			.body(problem);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.FORBIDDEN, "Acceso denegado", request);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.NOT_FOUND, "Recurso no encontrado", request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException ex,
		HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST, "Cuerpo de la petición inválido", request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});

		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Errores de validación", request);
		problem.setProperty("error", "Validation Failed");
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
		HttpServletRequest request) {
		return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
		HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST,
			"Valor inválido para '" + ex.getName() + "'", request);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ProblemDetail> handleUploadTooLarge(MaxUploadSizeExceededException ex,
		HttpServletRequest request) {
		return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE,
			"El archivo supera el tamaño máximo permitido", request);
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ProblemDetail> handleMissingPart(MissingServletRequestPartException ex,
		HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST,
			"Falta el archivo en la petición", request);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException ex,
		HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST,
			"Falta el parámetro '" + ex.getParameterName() + "'", request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ProblemDetail> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
		HttpServletRequest request) {
		return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
			"Tipo de contenido no soportado", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleGeneral(Exception ex, HttpServletRequest request) {
		log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request);
	}

	private ResponseEntity<ProblemDetail> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
		return ResponseEntity.status(status).body(problem(status, message, request));
	}

	private ProblemDetail problem(HttpStatus status, String message, HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
		problem.setTitle(status.getReasonPhrase());
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("timestamp", LocalDateTime.now());
		problem.setProperty("error", status.getReasonPhrase());
		problem.setProperty("message", message);
		problem.setProperty("path", request.getRequestURI());
		return problem;
	}
}
