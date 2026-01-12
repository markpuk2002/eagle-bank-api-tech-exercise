package com.eaglebank.exception;

/**
 * Exception thrown when validation fails for input data.
 * Typically used for enum conversion failures or invalid value assignments.
 */
public class ValidationException extends RuntimeException {
    
    /**
     * Creates a new ValidationException with the specified message.
     *
     * @param message The detail message explaining the validation failure
     */
    public ValidationException(String message) {
        super(message);
    }
    
    /**
     * Creates a new ValidationException with the specified message and cause.
     *
     * @param message The detail message explaining the validation failure
     * @param cause The cause of the exception (typically the original exception)
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
