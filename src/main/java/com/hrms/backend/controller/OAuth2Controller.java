package com.hrms.backend.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hrms.backend.dto.JwtAuthenticationResponse;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
public class OAuth2Controller {

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/success")
    public JwtAuthenticationResponse handleGoogleLogin(@RequestBody Map<String, String> requestBody) {
        String googleToken = requestBody.get("token");

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory()
        )
                .setAudience(Collections.singletonList("777107196271-smequh3mvjhb5m7ttkovqcepdqau55i8.apps.googleusercontent.com"))
                .build();

        try {
            GoogleIdToken idToken = verifier.verify(googleToken);
            if (idToken == null) {
                throw new GenericException("Invalid Google token", HttpStatus.FORBIDDEN);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            // Validate email
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new GenericException("Google email not verified", HttpStatus.FORBIDDEN);
            }

            String email = payload.getEmail();
            return authenticationService.handleOAuth2Login(email);
        } catch (Exception e) {
            throw new GenericException("Google token verification failed", HttpStatus.FORBIDDEN);
        }
    }

}