package com.hrms.backend.services;

public interface PasswordResetService {
    void createPasswordResetTokenForUser(String email);
    boolean resetPassword(String token, String newPassword);

}
