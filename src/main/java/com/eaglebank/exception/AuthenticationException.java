package com.eaglebank.exception;

/**
 * Exception thrown when authentication fails.
 * This is distinct from {@link UnauthorizedException} which is used for authorization failures.
 * Authentication failures (invalid credentials) should return HTTP 401 Unauthorized,
 * while authorization failures (access denied) should return HTTP 403 Forbidden.
 */
public class AuthenticationException extends RuntimeException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
