package com.hrms.backend.services.impl;

import com.hrms.backend.dto.JwtAuthenticationResponse;
import com.hrms.backend.dto.LoginRequest;
import com.hrms.backend.dto.SignUpRequest;
import com.hrms.backend.entities.Role;
import com.hrms.backend.entities.User;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.AuthenticationService;
import com.hrms.backend.services.JWTService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    // Explicit constructor to inject dependencies
    @Autowired
    public AuthenticationServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, @Qualifier("JWTServiceImpl")  JWTService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    // Method to handle signup logic
    public User signup(SignUpRequest signUpRequest) {
        User user = new User();
        user.setEmail(signUpRequest.getEmail());
        user.setName(signUpRequest.getUserName());
        user.setRole(Role.Admin); // Or assign another role
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword())); // Encode password
        return userRepository.save(user); // Save the user to the repository
    }


    //method to handle login logic
    // Login method to authenticate user and return JWT and Refresh Token
    public JwtAuthenticationResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(), loginRequest.getPassword()
        ));

        var user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        JwtAuthenticationResponse jwtAuthenticationResponse = new JwtAuthenticationResponse();
        try {
            var jwt = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(new HashMap<>(), user);

            jwtAuthenticationResponse.setToken(jwt);
            jwtAuthenticationResponse.setRefreshToken(refreshToken);
        }catch (Exception e){
            e.printStackTrace();
        }
        return jwtAuthenticationResponse;
    }
}
