package com.hrms.backend.services;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    UserDetailsService userDetailsService();

    // New method to process OAuth2User
    String processOAuth2User(OAuth2User oauth2User);
}
