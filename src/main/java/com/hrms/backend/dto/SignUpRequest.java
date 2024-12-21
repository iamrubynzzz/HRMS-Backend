package com.hrms.backend.dto;

public class SignUpRequest {

    private String email;
    private String userName;
    private String password;

    // Getter for email
    public String getEmail() {
        return email;
    }

    // Setter for email
    public void setEmail(String email) {
        this.email = email;
    }

    // Getter for name
    public String getUserName() {
        return userName;
    }

    // Setter for name
    public void setUserName(String userName) {
        this.userName = userName;
    }

    // Getter for password
    public String getPassword() {
        return password;
    }

    // Setter for password
    public void setPassword(String password) {
        this.password = password;
    }
}
