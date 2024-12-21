package com.hrms.backend.services;

import com.hrms.backend.dto.JwtAuthenticationResponse;
import com.hrms.backend.dto.LoginRequest;
import com.hrms.backend.dto.SignUpRequest;
import com.hrms.backend.entities.User;

public interface AuthenticationService {
    User signup(SignUpRequest signUpRequest);
    JwtAuthenticationResponse login(LoginRequest loginRequest);
}
