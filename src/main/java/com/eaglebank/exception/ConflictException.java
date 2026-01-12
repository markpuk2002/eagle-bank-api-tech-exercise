package com.eaglebank.exception;

/**
 * Exception thrown when a requested operation conflicts with the current state of a resource.
 * Typically used for business rule violations (e.g., deleting a user with associated accounts).
 */
public class ConflictException extends RuntimeException {
    
    /**
     * Creates a new ConflictException with the specified message.
     *
     * @param message The detail message explaining the conflict
     */
    public ConflictException(String message) {
        super(message);
    }
    
    /**
     * Creates a new ConflictException with the specified message and cause.
     *
     * @param message The detail message explaining the conflict
     * @param cause The cause of the exception
     */
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
