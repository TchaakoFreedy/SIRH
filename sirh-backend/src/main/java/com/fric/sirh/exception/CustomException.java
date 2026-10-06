package com.fric.sirh.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public CustomException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.code = "BAD_REQUEST";
    }

    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.code = status.name();
    }

    public CustomException(String message, String code, HttpStatus status) {
        super(message);
        this.status = status;
        this.code = code;
    }
}