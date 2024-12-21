package com.hrms.backend.controller;

import com.hrms.backend.dto.JwtAuthenticationResponse;
import com.hrms.backend.dto.LoginRequest;
import com.hrms.backend.dto.SignUpRequest;
import com.hrms.backend.entities.User;
import com.hrms.backend.services.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    // Constructor injection
    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<User> signup(@RequestBody SignUpRequest signUpRequest) {
        return ResponseEntity.ok(authenticationService.signup(signUpRequest));
    }

    // Endpoint for login and JWT generation
    @PostMapping("/login")
    public ResponseEntity<JwtAuthenticationResponse> login(@RequestBody LoginRequest loginRequest) {
        JwtAuthenticationResponse response = authenticationService.login(loginRequest);
        return ResponseEntity.ok(response); // Return JWT and refresh token
    }
}
