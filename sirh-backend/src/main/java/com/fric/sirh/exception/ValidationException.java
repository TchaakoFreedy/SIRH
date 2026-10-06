package com.fric.sirh.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ValidationException extends BusinessException {
    private final Map<String, String> validationErrors;

    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
        this.validationErrors = null;
    }

    public ValidationException(String message, Map<String, String> validationErrors) {
        super(message, HttpStatus.BAD_REQUEST);
        this.validationErrors = validationErrors;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
}