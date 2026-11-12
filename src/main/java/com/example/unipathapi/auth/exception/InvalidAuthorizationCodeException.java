package com.example.unipathapi.auth.exception;

public class InvalidAuthorizationCodeException extends GoogleAuthException {
    public InvalidAuthorizationCodeException(String message) {
        super(message);
    }

    public InvalidAuthorizationCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}

