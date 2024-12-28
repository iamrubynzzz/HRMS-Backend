package com.hrms.backend.services.impl;

import com.hrms.backend.entities.Role;
import com.hrms.backend.entities.User;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.JWTService;
import com.hrms.backend.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private JWTService jwtService;
    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder;
    @Autowired
    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, @Qualifier("JWTServiceImpl") JWTService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // Existing UserDetailsService implementation
    @Override
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    // New method to process OAuth2User
    @Override
    public String processOAuth2User(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        // Check if the user already exists
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            System.out.println("User already exists: " + email);

            // Check user's role and process accordingly
            if (user.getRole() == Role.ADMIN) {
                System.out.println("Processing as ADMIN user...");

            } else if (user.getRole() == Role.MANAGER) {
                System.out.println("Processing as MANAGER user...");

            } else if (user.getRole() == Role.EMPLOYEE) {
                System.out.println("Processing as EMPLOYEE user...");

            } else {
                System.out.println("Unknown role for user. Assigning default role.");
                user.setRole(Role.EMPLOYEE); // Default fallback
            }
        } else {
            // Create a new user with a default role and random password
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setPassword(passwordEncoder.encode("oauth2user")); // Placeholder password
            newUser.setRole(Role.EMPLOYEE); // Default role
            user = userRepository.save(newUser);
            System.out.println("New user created: " + email);
        }

        // Generate and return a JWT token for the user
        return jwtService.generateToken(user);
    }


}
