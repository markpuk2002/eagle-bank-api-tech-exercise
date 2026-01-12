package com.eaglebank.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resource, String identifier) {
        super(String.format("%s not found: %s", resource, identifier));
    }
}
