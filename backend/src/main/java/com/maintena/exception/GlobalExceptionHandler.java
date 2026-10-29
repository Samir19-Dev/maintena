package com.maintena.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		ErrorResponse error = ErrorResponse.builder()
			.timestamp(LocalDateTime.now().format(FORMATTER))
			.status(HttpStatus.NOT_FOUND.value())
			.error("Not Found")
			.message(ex.getMessage())
			.path(request.getRequestURI())
			.details(new ArrayList<>())
			.build();
		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
		ErrorResponse error = ErrorResponse.builder()
			.timestamp(LocalDateTime.now().format(FORMATTER))
			.status(HttpStatus.BAD_REQUEST.value())
			.error("Bad Request")
			.message(ex.getMessage())
			.path(request.getRequestURI())
			.details(new ArrayList<>())
			.build();
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ApiError.class)
	public ResponseEntity<ErrorResponse> handleApiError(ApiError ex, HttpServletRequest request) {
		ErrorResponse error = ErrorResponse.builder()
			.timestamp(LocalDateTime.now().format(FORMATTER))
			.status(HttpStatus.BAD_REQUEST.value())
			.error("Bad Request")
			.message(ex.getMessage())
			.path(request.getRequestURI())
			.details(new ArrayList<>())
			.build();
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<String> details = new ArrayList<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			details.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
		}
		ErrorResponse error = ErrorResponse.builder()
			.timestamp(LocalDateTime.now().format(FORMATTER))
			.status(HttpStatus.BAD_REQUEST.value())
			.error("Validation Failed")
			.message("Errores de validación en los campos")
			.path(request.getRequestURI())
			.details(details)
			.build();
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGlobal(Exception ex, HttpServletRequest request) {
		ErrorResponse error = ErrorResponse.builder()
			.timestamp(LocalDateTime.now().format(FORMATTER))
			.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
			.error("Internal Server Error")
			.message("Ha ocurrido un error interno en el servidor")
			.path(request.getRequestURI())
			.details(new ArrayList<>())
			.build();
		return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
