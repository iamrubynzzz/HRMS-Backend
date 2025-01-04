package com.hrms.backend.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationCredentialsNotFoundException extends RuntimeException{
    private final HttpStatus status;

    public AuthenticationCredentialsNotFoundException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
