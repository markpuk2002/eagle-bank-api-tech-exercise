package com.eaglebank.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class BadRequestErrorResponse {
    
    private String message;
    private List<ErrorDetail> details;

    public BadRequestErrorResponse() {
    }

    public BadRequestErrorResponse(String message, List<ErrorDetail> details) {
        this.message = message;
        this.details = details;
    }

    public static class ErrorDetail {
        private String field;
        private String message;
        private String type;
        
        public ErrorDetail() {
        }
        
        public ErrorDetail(String field, String message, String type) {
            this.field = field;
            this.message = message;
            this.type = type;
        }
        
        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
