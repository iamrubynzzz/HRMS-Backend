package com.hrms.backend;

import com.hrms.backend.entities.Role;
import com.hrms.backend.entities.User;
import com.hrms.backend.entities.Status;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@EnableAsync
@SpringBootApplication
@EnableScheduling
public class HrmsApplication implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("rubina123")
    private String adminPassword;

    public static void main(String[] args) {
        SpringApplication.run(HrmsApplication.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            boolean superAdminExists = userRepository.existsByRole(Role.SUPER_ADMIN);
            if (!superAdminExists) {
                User superAdminUser = User.builder()
                        .email(adminEmail)
                        .name("Rubina Thapa")
                        .role(Role.SUPER_ADMIN)
                        .password(passwordEncoder.encode(adminPassword))
                        .status(Status.APPROVED)
                        .build();

                userRepository.save(superAdminUser);
                System.out.println("Super Admin user created successfully.");
            } else {
                System.out.println("Super Admin user already exists.");
            }
        } catch (Exception e) {
            throw new GenericException("Error creating Super admin user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
