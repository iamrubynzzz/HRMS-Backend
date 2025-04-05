package com.hrms.backend.services;

import com.hrms.backend.dto.ChangePasswordRequest;

public interface PasswordResetService {
    void createPasswordResetTokenForUser(String email);
    boolean resetPassword(String token, String newPassword);

    boolean changePassword(ChangePasswordRequest request);

}
