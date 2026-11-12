package com.example.unipathapi.auth.exception;

public class InvalidGoogleTokenException extends GoogleAuthException {
    public InvalidGoogleTokenException(String message) {
        super(message);
    }

    public InvalidGoogleTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}

