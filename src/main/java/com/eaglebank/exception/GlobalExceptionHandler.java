package com.eaglebank.exception;

import com.eaglebank.dto.response.BadRequestErrorResponse;
import com.eaglebank.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST controllers.
 * Provides centralized exception handling with appropriate HTTP status codes and error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(ex.getMessage(), "AUTHENTICATION_FAILED");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(ex.getMessage(), "UNAUTHORIZED");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.debug("Resource not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(ex.getMessage(), "RESOURCE_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(DuplicateResourceException ex) {
        log.debug("Duplicate resource: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(ex.getMessage(), "DUPLICATE_RESOURCE");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(ex.getMessage(), "CONFLICT");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(InsufficientFundsException ex) {
        log.warn("Insufficient funds: account {}, requested: {}, balance: {}", 
            ex.getAccountNumber(), ex.getRequestedAmount(), ex.getCurrentBalance());
        ErrorResponse error = new ErrorResponse(ex.getMessage(), "INSUFFICIENT_FUNDS");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        log.debug("Validation error: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(ex.getMessage(), "VALIDATION_ERROR");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.debug("Illegal argument: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(ex.getMessage(), "ILLEGAL_ARGUMENT");
        
        // Determine status code based on message (fallback for edge cases)
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = ex.getMessage().toLowerCase();
        if (message.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        } else if (message.contains("does not have access") || 
                   message.contains("does not own") ||
                   message.contains("access token is missing or invalid")) {
            status = HttpStatus.FORBIDDEN;
        }
        
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        log.debug("Response status exception: {}", ex.getReason());
        ErrorResponse error = new ErrorResponse(ex.getReason(), "RESPONSE_STATUS_ERROR");
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        
        // Extract meaningful error message
        String message = "Data integrity violation";
        String rootCause = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        
        if (rootCause != null) {
            if (rootCause.contains("Duplicate entry")) {
                message = "A resource with this value already exists";
            } else if (rootCause.contains("foreign key constraint")) {
                message = "Cannot perform operation due to related resources";
            } else if (rootCause.contains("unique constraint")) {
                message = "A resource with this value already exists";
            }
        }
        
        ErrorResponse error = new ErrorResponse(message, "DATA_INTEGRITY_VIOLATION");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        log.debug("Constraint violation: {}", ex.getMessage());
        
        String violations = ex.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining(", "));
        
        ErrorResponse error = new ErrorResponse(
            "Validation failed: " + violations, 
            "CONSTRAINT_VIOLATION"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.debug("HTTP message not readable: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            "Malformed request body. Please check the JSON format and required fields.",
            "MALFORMED_REQUEST"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.debug("Method argument type mismatch: {}", ex.getMessage());
        
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
            ex.getValue(),
            ex.getName(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        ErrorResponse error = new ErrorResponse(message, "INVALID_ARGUMENT_TYPE");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.debug("Missing request parameter: {}", ex.getParameterName());
        ErrorResponse error = new ErrorResponse(
            String.format("Missing required parameter: %s", ex.getParameterName()),
            "MISSING_PARAMETER"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        log.debug("Missing request header: {}", ex.getHeaderName());
        ErrorResponse error = new ErrorResponse(
            "Access token is missing or invalid",
            "ILLEGAL_ARGUMENT"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BadRequestErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<BadRequestErrorResponse.ErrorDetail> details = new ArrayList<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.add(new BadRequestErrorResponse.ErrorDetail(
                fieldName,
                errorMessage != null ? errorMessage : "Validation failed",
                "validation_error"
            ));
        });

        BadRequestErrorResponse response = new BadRequestErrorResponse(
            "Validation failed",
            details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // Log full exception details internally (for debugging)
        log.error("Unexpected error occurred", ex);
        
        // Return generic message to client (no internal details)
        ErrorResponse error = new ErrorResponse(
            "An unexpected error occurred. Please contact support if this persists.",
            "INTERNAL_SERVER_ERROR"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}