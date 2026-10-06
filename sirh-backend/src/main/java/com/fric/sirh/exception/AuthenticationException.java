package com.fric.sirh.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends BusinessException {

    public AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    public AuthenticationException() {
        super("Authentification échouée", HttpStatus.UNAUTHORIZED);
    }
}