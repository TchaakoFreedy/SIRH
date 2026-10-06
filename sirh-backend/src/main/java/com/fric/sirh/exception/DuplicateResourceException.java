package com.fric.sirh.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    public DuplicateResourceException(String resource, String field, String value) {
        super(String.format("Un(e) %s avec %s '%s' existe déjà", resource, field, value), HttpStatus.CONFLICT);
    }
}