package com.eaglebank.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Standard error response for API errors.
 * Contains error message, optional error code, and timestamp.
 */
@Data
public class ErrorResponse {
    
    private String message;
    private String errorCode;
    private OffsetDateTime timestamp;
    
    public ErrorResponse() {
        this.timestamp = OffsetDateTime.now();
    }
    
    public ErrorResponse(String message) {
        this();
        this.message = message;
    }
    
    public ErrorResponse(String message, String errorCode) {
        this();
        this.message = message;
        this.errorCode = errorCode;
    }
}
