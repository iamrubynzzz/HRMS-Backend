package com.hrms.backend;

import com.hrms.backend.entities.Role;
import com.hrms.backend.entities.User;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.exception.UserCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class HrmsApplication implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public static void main(String[] args) {
        SpringApplication.run(HrmsApplication.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            // Check if an admin user already exists
            boolean adminExists = userRepository.existsByRole(Role.ADMIN);
            if (!adminExists) {
                // Create an admin user if not present
                User adminUser = User.builder()
                        .email("rubynzzz@gmail.com")  // Admin email
                        .name("Rubina Thapa")         // Admin name
                        .role(Role.ADMIN)             // Set the role to Admin
                        .password(passwordEncoder.encode("rubina123")) // Encrypted password
                        .build();

                // Save the Admin user to the database
                userRepository.save(adminUser);
                System.out.println("Admin user created successfully.");
            } else {
                System.out.println("Admin user already exists.");
            }
        } catch (Exception e) {
            // Handle exceptions during the admin creation process
            throw new UserCreationException("Error creating Admin user: " + e.getMessage(), e);
        }
    }
}
