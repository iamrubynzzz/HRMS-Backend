package com.hrms.backend.dto;

public class LoginRequest {

    private String email;
    private String password;

    // Explicit getter for email
    public String getEmail() {
        return email;
    }

    // Explicit setter for email
    public void setEmail(String email) {
        this.email = email;
    }

    // Explicit getter for password
    public String getPassword() {
        return password;
    }

    // Explicit setter for password
    public void setPassword(String password) {
        this.password = password;
    }
}
