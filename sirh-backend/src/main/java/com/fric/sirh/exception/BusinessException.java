package com.fric.sirh.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.code = "BAD_REQUEST";
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.code = status.name();
    }

    public BusinessException(String message, String code, HttpStatus status) {
        super(message);
        this.status = status;
        this.code = code;
    }
}