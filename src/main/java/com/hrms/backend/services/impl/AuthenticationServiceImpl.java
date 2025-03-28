package com.hrms.backend.services.impl;

import com.hrms.backend.dto.JwtAuthenticationResponse;
import com.hrms.backend.dto.LoginRequest;
import com.hrms.backend.dto.RefreshTokenRequest;
import com.hrms.backend.dto.SignUpRequest;
import com.hrms.backend.entities.*;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.repository.CompanyRepository;
import com.hrms.backend.repository.TokenRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.AuthenticationService;
import com.hrms.backend.services.JWTService;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final CompanyRepository companyRepository;
    private final TokenRepository tokenRepository;


    @Autowired
    public AuthenticationServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                     AuthenticationManager authenticationManager,
                                     @Qualifier("JWTServiceImpl") JWTService jwtService, CompanyRepository companyRepository, TokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.companyRepository = companyRepository;
        this.tokenRepository = tokenRepository;
    }



    //Sign up logic
    public User signup(SignUpRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new GenericException("User already exists with this email", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setEmail(signUpRequest.getEmail());
        user.setName(signUpRequest.getName());
        user.setRole(signUpRequest.getRole());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        return userRepository.save(user);
    }

    // Login method to authenticate user and return JWT
    public JwtAuthenticationResponse login(LoginRequest loginRequest) {
        try {
            System.out.println("Attempting authentication for: " + loginRequest.getEmail());

            // Find the user by email
            var user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new GenericException("Invalid email or password.", HttpStatus.UNAUTHORIZED));

            // Check if the user is approved
            if (user.getStatus() != Status.APPROVED) {
                System.out.println("User is not approved: " + user.getEmail());
                throw new GenericException("Your account is not approved yet. Please wait for approval.", HttpStatus.FORBIDDEN);
            }

            // Skip company validation for Super Admin
            if (user.getRole() != Role.SUPER_ADMIN) {
                // For Admin, Manager, and Employee, company name is required
                if (loginRequest.getCompanyName() == null || loginRequest.getCompanyName().isEmpty()) {
                    throw new GenericException("Company name is required for login.", HttpStatus.BAD_REQUEST);
                }

                // Validate the company
                Company company = companyRepository.findByName(loginRequest.getCompanyName())
                        .orElseThrow(() -> new GenericException("Invalid company name.", HttpStatus.BAD_REQUEST));

                // Check if the user belongs to the specified company
                if (!user.getCompany().equals(company)) {
                    throw new GenericException("User does not belong to the specified company.", HttpStatus.FORBIDDEN);
                }
            }

            // Authenticate the user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(), loginRequest.getPassword()
                    )
            );

            System.out.println("Authentication successful, generating tokens...");

            // Revoke old tokens to ensure only the new token is valid
            revokeAllTokensByUser(user);

            // Generate new JWT and refresh token
            var jwt = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(new HashMap<>(), user);
            
            saveUserToken(user,jwt,refreshToken);

            JwtAuthenticationResponse jwtAuthenticationResponse = new JwtAuthenticationResponse();
            jwtAuthenticationResponse.setToken(jwt);
            jwtAuthenticationResponse.setRefreshToken(refreshToken);

            return jwtAuthenticationResponse;

        } catch (BadCredentialsException e) {
            throw new GenericException("Invalid email or password.", HttpStatus.UNAUTHORIZED);
        } catch (GenericException e) {
            throw e; // Re-throw the GenericException
        } catch (Exception e) {
            e.printStackTrace();
            throw new GenericException("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Logout method
    public void logout(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new GenericException("Invalid Token", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GenericException("User not found", HttpStatus.NOT_FOUND));

        // Revoke all active tokens for the user
        revokeAllTokensByUser(user);
    }

    @Override
    public JwtAuthenticationResponse handleOAuth2Login(String email) throws GenericException {
        // Find the user by email
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GenericException("User not registered. Please contact your admin to create an account.", HttpStatus.FORBIDDEN));

        // Check if the user is approved
        if (user.getStatus() != Status.APPROVED) {
            throw new GenericException("Your account is not approved yet. Please wait for approval.", HttpStatus.FORBIDDEN);
        }

        // Revoke old tokens to ensure only the new token is valid
        revokeAllTokensByUser(user);

        // Generate new JWT and refresh token
        var jwt = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(new HashMap<>(), user);

        saveUserToken(user, jwt, refreshToken);

        JwtAuthenticationResponse jwtAuthenticationResponse = new JwtAuthenticationResponse();
        jwtAuthenticationResponse.setToken(jwt);
        jwtAuthenticationResponse.setRefreshToken(refreshToken);
        jwtAuthenticationResponse.setRole(user.getRole());

        return jwtAuthenticationResponse;
    }

    // Helper method to store user tokens
    private void saveUserToken(User user, String accessToken, String refreshToken) {
        Token token = new Token();
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setLoggedOut(false);
        token.setUser(user);
        tokenRepository.save(token);
    }

    // Helper method to revoke all active tokens for a user
    private void revokeAllTokensByUser(User user) {
        // Fetch all active tokens for the user (tokens that are not logout)
        List<Token> validTokens = tokenRepository.findAllByUserAndLoggedOutFalse(user);

        // If there are no active tokens, return early
        if (validTokens.isEmpty()) {
            return;
        }

        // Mark all active tokens as logout
        validTokens.forEach(t -> {
            t.setLoggedOut(true);  // Mark token as logged out
        });

        // Save the updated tokens to the database
        tokenRepository.saveAll(validTokens);
    }


    //Method for refresh token
    public JwtAuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        try {
            // Extract the email of the user from the refresh token
            String userEmail = jwtService.extractUsername(refreshTokenRequest.getToken());

            // Find the user associated with the email
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new GenericException("User not found", HttpStatus.UNAUTHORIZED));

            // Validate the refresh token
            if (!jwtService.isTokenValid(refreshTokenRequest.getToken(), user)) {
                throw new GenericException("The refresh token is invalid or expired", HttpStatus.UNAUTHORIZED);
            }

            // Generate new JWT token and return response with the old refresh token
            var jwt = jwtService.generateToken(user);
            JwtAuthenticationResponse jwtAuthenticationResponse = new JwtAuthenticationResponse();
            jwtAuthenticationResponse.setToken(jwt);
            jwtAuthenticationResponse.setRefreshToken(refreshTokenRequest.getToken());

            return jwtAuthenticationResponse;

        } catch (GenericException e) {
            throw e;
        } catch (JwtException e) {  // Catch JWT-specific exceptions
            throw new GenericException("Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            // Handle other unexpected errors
            throw new GenericException("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
