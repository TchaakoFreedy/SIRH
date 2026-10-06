package com.fric.sirh.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedAccessException extends BusinessException {

    public UnauthorizedAccessException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

    public UnauthorizedAccessException() {
        super("Accès non autorisé", HttpStatus.FORBIDDEN);
    }
}