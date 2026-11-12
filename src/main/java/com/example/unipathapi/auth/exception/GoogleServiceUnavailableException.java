package com.example.unipathapi.auth.exception;

public class GoogleServiceUnavailableException extends GoogleAuthException {
    public GoogleServiceUnavailableException(String message) {
        super(message);
    }

    public GoogleServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

