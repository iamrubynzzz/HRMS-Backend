package com.hrms.backend.controller;

import com.hrms.backend.dto.*;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.Role;
import com.hrms.backend.entities.Salary;
import com.hrms.backend.entities.User;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.repository.AttendanceRepository;
import com.hrms.backend.repository.SalaryRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.AttendanceService;
import com.hrms.backend.services.AuthenticationService;
import com.hrms.backend.services.EmailService;
import com.hrms.backend.services.SalaryService;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final AttendanceService attendanceService;
    private final EmailService emailService;
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignUpRequest signUpRequest) {
        try {
            Role role = Role.fromString(String.valueOf(signUpRequest.getRole()));

            if (role == Role.ADMIN) {
                return ResponseEntity.badRequest().body("Admin role is not allowed for signup.");
            }

            // Set the valid role in the signup request
            signUpRequest.setRole(role);

            // Proceed with signup logic
            User user = authenticationService.signup(signUpRequest);

            // Return success message with user details
            return ResponseEntity.ok("User " + user.getName() + " registered successfully as " + user.getRole().name());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid role provided: " + signUpRequest.getRole());
        }
    }

    // Endpoint for login and JWT generation
    @PostMapping("/login")
    public ResponseEntity<JwtAuthenticationResponse> login(@RequestBody LoginRequest loginRequest) {
        JwtAuthenticationResponse response = authenticationService.login(loginRequest);
        return ResponseEntity.ok(response); // Return JWT and refresh token
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        authenticationService.logout(request);
        return ResponseEntity.ok("Logout successful");
    }


    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthenticationResponse> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authenticationService.refreshToken(refreshTokenRequest));
    }

    @GetMapping("/error")
    public ResponseEntity<String> handleError() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Authentication failed. Please try again.");
    }

}
