package com.hrms.backend.services;

import com.hrms.backend.dto.JwtAuthenticationResponse;
import com.hrms.backend.dto.LoginRequest;
import com.hrms.backend.dto.RefreshTokenRequest;
import com.hrms.backend.dto.SignUpRequest;
import com.hrms.backend.entities.User;
import com.hrms.backend.exception.GenericException;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthenticationService {
    User signup(SignUpRequest signUpRequest);
    JwtAuthenticationResponse login(LoginRequest loginRequest);
    JwtAuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    void logout(HttpServletRequest request);

    JwtAuthenticationResponse handleOAuth2Login(String email) throws GenericException;;
}
