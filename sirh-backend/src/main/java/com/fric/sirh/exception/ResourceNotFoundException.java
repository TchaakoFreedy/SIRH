package com.fric.sirh.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String resource, String id) {
        super(String.format("%s non trouvé avec l'ID: %s", resource, id), HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String resource, String field, String value) {
        super(String.format("%s non trouvé avec %s: %s", resource, field, value), HttpStatus.NOT_FOUND);
    }
}