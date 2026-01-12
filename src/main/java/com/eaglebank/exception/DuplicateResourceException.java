package com.eaglebank.exception;

/**
 * Exception thrown when attempting to create or update a resource that already exists.
 * Typically used for unique constraint violations (e.g., duplicate username, email).
 */
public class DuplicateResourceException extends RuntimeException {
    
    /**
     * Creates a new DuplicateResourceException with the specified message.
     *
     * @param message The detail message
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
    
    /**
     * Creates a new DuplicateResourceException for a duplicate resource.
     *
     * @param resource The type of resource (e.g., "User", "Username")
     * @param identifier The identifier that already exists (e.g., username, email)
     */
    public DuplicateResourceException(String resource, String identifier) {
        super(String.format("%s already exists: %s", resource, identifier));
    }
    
    /**
     * Creates a new DuplicateResourceException with the specified message and cause.
     *
     * @param message The detail message
     * @param cause The cause of the exception
     */
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
