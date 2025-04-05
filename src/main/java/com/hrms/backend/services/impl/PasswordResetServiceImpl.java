package com.hrms.backend.services.impl;

import com.hrms.backend.dto.ChangePasswordRequest;
import com.hrms.backend.entities.EmailMessage;
import com.hrms.backend.entities.EmailStatus;
import com.hrms.backend.entities.PasswordResetToken;
import com.hrms.backend.entities.User;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.repository.EmailMessageRepository;
import com.hrms.backend.repository.PasswordResetTokenRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.EmailService;
import com.hrms.backend.services.PasswordResetService;
import com.hrms.backend.services.UserService;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {
    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailMessageRepository emailMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    private static final int EXPIRY_TIME_IN_MINUTES = 40;

    public void createPasswordResetTokenForUser(String email) {
        //  Generate the token
        String token = UUID.randomUUID().toString();

        // Create and save the password reset token
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken(token);
        passwordResetToken.setEmail(email);
        passwordResetToken.setExpiryDate(LocalDateTime.now().plusMinutes(EXPIRY_TIME_IN_MINUTES));

        tokenRepository.save(passwordResetToken);

        //  Construct the reset link
        String resetLink = "http://localhost:3000/reset-password?token=" + token;

        // Construct the email content
        String subject = "Password Reset Request";
        String message = "<p>Click <a href='" + resetLink + "'>here</a> to reset your password.</p>";

        // Create EmailMessage entity and save it to DB
        EmailMessage emailMessage = new EmailMessage();
        emailMessage.setRecipientAddress(email);
        emailMessage.setSubject(subject);
        emailMessage.setMessage(message);
        emailMessage.setStatus(EmailStatus.PENDING);  // Email status initially set to PENDING
        emailMessage.setCompanyName("Your Company Name");  // Set this as per your requirement
        emailMessageRepository.save(emailMessage);  // Save to DB

        // Step 6: Send the reset email asynchronously
        try {
            // Send the email asynchronously and handle status updates in the future
            CompletableFuture<Boolean> future = emailService.sendEmail(email, subject, message);
            future.thenAccept(sent -> {
                if (sent) {
                    // Update email status to SUCCESS if sent successfully
                    emailMessageRepository.updateEmailStatusById(emailMessage.getId(), EmailStatus.SUCCESS);
                    System.out.println("Password reset email sent to " + email);
                } else {
                    // Update email status to FAILED if sending failed
                    emailMessageRepository.updateEmailStatusById(emailMessage.getId(), EmailStatus.FAILED);
                    System.out.println("Failed to send password reset email to " + email);
                }
            });
        } catch (MessagingException e) {
            // Handle error if the email sending fails
            emailMessageRepository.updateEmailStatusById(emailMessage.getId(), EmailStatus.FAILED);
            System.out.println("Error sending password reset email: " + e.getMessage());
            e.printStackTrace();
        }
    }




    public boolean resetPassword(String token, String newPassword) {
        System.out.println("Received token: [" + token + "]");
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        if (resetToken.isExpired()) {
            throw new RuntimeException("Token has expired");
        }

        // Fetch the user by email (assuming email == username in your case)
        Optional<User> userOptional = userService.findByUsername(resetToken.getEmail());

        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOptional.get();

        // Log old password
        System.out.println("Old Password (Hashed): " + user.getPassword());

        // Update the password
        userService.updatePassword(user.getUsername(), newPassword);

        // Fetch updated user to confirm change
        Optional<User> updatedUserOptional = userService.findByUsername(user.getUsername());

        if (updatedUserOptional.isPresent()) {
            System.out.println("New Password (Hashed): " + updatedUserOptional.get().getPassword());
        } else {
            System.out.println("Error: User not found after update!");
        }

        // Delete the token after use
        tokenRepository.delete(resetToken);

        return true;
    }

    // Existing method to fetch authenticated user
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    // Change Password Method
    @Transactional
    public boolean changePassword(ChangePasswordRequest request) {
        // Fetch the currently authenticated user
        User user = getAuthenticatedUser();

        // Validate Old Password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new GenericException("Old password is incorrect", HttpStatus.BAD_REQUEST);
        }

        // Validate if New Password and Confirm Password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new GenericException("New password and confirm password do not match", HttpStatus.BAD_REQUEST);
        }

        // Encrypt and save the new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return true;
    }


}

