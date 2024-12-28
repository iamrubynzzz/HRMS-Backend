package com.hrms.backend.exception;

public class UserCreationException extends RuntimeException {
    public UserCreationException(String message, Exception e) {
        super(message);
    }
}
