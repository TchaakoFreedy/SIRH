// src/main/java/com/fric/sirh/exception/ContractConflictException.java
package com.fric.sirh.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ContractConflictException extends RuntimeException {

    public ContractConflictException(String message) {
        super(message);
    }

    public ContractConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}