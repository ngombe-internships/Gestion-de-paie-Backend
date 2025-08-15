package com.hades.maalipo.exception;

public class CongeValidationException extends RuntimeException {

    public CongeValidationException(String message) {
        super(message);
    }

    public CongeValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
