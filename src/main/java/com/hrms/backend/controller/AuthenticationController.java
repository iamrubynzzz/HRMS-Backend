package com.hrms.backend.controller;

import com.hrms.backend.dto.JwtAuthenticationResponse;
import com.hrms.backend.dto.LoginRequest;
import com.hrms.backend.dto.RefreshTokenRequest;
import com.hrms.backend.dto.SignUpRequest;
import com.hrms.backend.entities.Role;
import com.hrms.backend.entities.User;
import com.hrms.backend.services.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignUpRequest signUpRequest) {
        try {
            Role role = Role.fromString(String.valueOf(signUpRequest.getRole()));

            // Check if the role is Admin, which should not be allowed for signup
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
        System.out.println("Received email: " + loginRequest.getEmail());
        System.out.println("Received password: " + loginRequest.getPassword());
        JwtAuthenticationResponse response = authenticationService.login(loginRequest);
        return ResponseEntity.ok(response); // Return JWT and refresh token
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
