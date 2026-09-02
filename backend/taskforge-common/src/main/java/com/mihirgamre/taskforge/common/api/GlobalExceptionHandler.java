package com.mihirgamre.taskforge.common.api;

import jakarta.validation.ConstraintViolationException;
import com.mihirgamre.taskforge.common.observability.CorrelationIdFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        ApiError error = new ApiError(java.time.Instant.now(), 400, "VALIDATION_FAILED",
                "The request failed validation.", details, requestId());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        List<String> details = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        ApiError error = new ApiError(java.time.Instant.now(), 400, "VALIDATION_FAILED",
                "The request failed validation.", details, requestId());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        ApiError error = ApiError.of(400, "MALFORMED_JSON", "The request body is not valid JSON.", requestId());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> handleBadRequest(Exception exception) {
        ApiError error = ApiError.of(400, "BAD_REQUEST", "The request is invalid.", requestId());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        ApiError error = ApiError.of(400, "BAD_REQUEST", exception.getMessage(), requestId());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String code = exception.getStatusCode().toString().replace(' ', '_');
        String message = exception.getReason() == null ? exception.getStatusCode().toString() : exception.getReason();
        ApiError error = ApiError.of(status, code, message, requestId());
        return ResponseEntity.status(exception.getStatusCode()).body(error);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ApiError> handleNotFound(Exception exception) {
        ApiError error = ApiError.of(404, "NOT_FOUND", "The requested resource was not found.", requestId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        log.error("Unhandled API exception", exception);
        ApiError error = ApiError.of(500, "INTERNAL_ERROR", "An unexpected error occurred.", requestId());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String requestId() {
        return MDC.get(CorrelationIdFilter.MDC_KEY);
    }
}
