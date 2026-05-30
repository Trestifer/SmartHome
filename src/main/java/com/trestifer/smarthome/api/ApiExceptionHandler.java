package com.trestifer.smarthome.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ResponseStatusException.class)
	ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
		return ResponseEntity
				.status(exception.getStatusCode())
				.body(new ApiError(exception.getReason()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
		FieldError fieldError = exception.getBindingResult().getFieldError();
		String message = fieldError == null ? "Dữ liệu không hợp lệ." : fieldError.getDefaultMessage();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> handleUnreadableJson() {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError("JSON request body không hợp lệ."));
	}
}
