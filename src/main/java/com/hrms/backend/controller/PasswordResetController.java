package com.hrms.backend.controller;

import com.hrms.backend.dto.ChangePasswordRequest;
import com.hrms.backend.dto.ForgotPasswordRequest;
import com.hrms.backend.entities.User;
import com.hrms.backend.services.PasswordResetService;
import com.hrms.backend.services.UserService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/password")
@CrossOrigin(origins = "http://localhost:3000")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private UserService userService;
    // Forgot Password - Generate Token and Send Reset Email
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        System.out.println("--------------------");
        String email = request.getEmail(); // Extract the email from the request
        passwordResetService.createPasswordResetTokenForUser(email);
        return ResponseEntity.ok("Password reset email sent.");
    }

    // Reset Password - After Clicking Reset Link
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        boolean isReset = passwordResetService.resetPassword(token, newPassword);
        if (isReset) {
            System.out.println("Password reset successful");
            return ResponseEntity.ok("Password reset successfully.");
        }
        System.out.println("Password reset failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to reset password.");
    }

    // New Change Password Endpoint
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        boolean isPasswordChanged = passwordResetService.changePassword(request);
        if (isPasswordChanged) {
            return ResponseEntity.ok("Password changed successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to change password.");
        }
    }
}
