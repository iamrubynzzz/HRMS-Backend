package com.hrms.backend.services.impl;

import com.hrms.backend.dto.UserDTO;  // Adding the UserDTO
import com.hrms.backend.dto.UserRequestDTO;
import com.hrms.backend.entities.*;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.repository.EmployeeManagerRepository;
import com.hrms.backend.repository.UserInfoRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.JWTService;
import com.hrms.backend.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private JWTService jwtService;
    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;

    private final BCryptPasswordEncoder passwordEncoder;

    private final EmployeeManagerRepository employeeManagerRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, @Qualifier("JWTServiceImpl") JWTService jwtService, UserInfoRepository userInfoRepository, EmployeeManagerRepository employeeManagerRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userInfoRepository = userInfoRepository;
        this.employeeManagerRepository = employeeManagerRepository;
    }

    // Existing UserDetailsService implementation
    @Override
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    // New method to process OAuth2User
    @Override
    public String processOAuth2User(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null || name == null) {
            throw new GenericException("OAuth2 user information is incomplete.", HttpStatus.BAD_REQUEST);
        }

        // Check if the user already exists
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            System.out.println("User already exists: " + email);

            // Handle user status
            if (user.getStatus() == UserStatus.PENDING) {
                System.out.println("User is pending approval.");
            }

            // Check user's role and process accordingly
            switch (user.getRole()) {
                case ADMIN -> System.out.println("Processing as ADMIN user...");
                case MANAGER -> System.out.println("Processing as MANAGER user...");
                case EMPLOYEE -> System.out.println("Processing as EMPLOYEE user...");
                default -> {
                    System.out.println("Unknown role for user. Assigning default role.");
                    user.setRole(Role.EMPLOYEE); // Default fallback
                }
            }
        } else {
            // Create a new user with a pending status and default role
            try {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setPassword(passwordEncoder.encode("oauth2user")); // Placeholder password
                newUser.setRole(Role.EMPLOYEE); // Default role
                newUser.setStatus(UserStatus.PENDING); // Set status as pending
                user = userRepository.save(newUser);
                System.out.println("New user created: " + email);
            } catch (Exception e) {
                throw new GenericException("Error occurred while creating a new user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // Generate and return a JWT token for the user
        try {
            return jwtService.generateToken(user);
        } catch (Exception e) {
            throw new GenericException("Error occurred while generating JWT token: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Implement the getUsersByStatus method
    @Override
    public List<UserDTO> getUsersByStatus(UserStatus userStatus) {
        List<User> users = userRepository.findByStatus(userStatus);

        //Case where no users are found in the given status
        if (users.isEmpty()) {
            throw new GenericException("No users found with status: " + userStatus, HttpStatus.NOT_FOUND);
        }

        // Convert the list of User entities to UserDTOs, excluding password
        return users.stream()
                .map(user -> new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus()))
                .collect(Collectors.toList());
    }

    // Method for admin to approve a user
    @Override
    public UserDTO approveUser(Integer userId, UserRequestDTO userInfo) {
        // Fetch the user by ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GenericException("User not found with ID: " + userId, HttpStatus.NOT_FOUND));

        // Check if the user is in PENDING status
        if (user.getStatus() != UserStatus.PENDING) {
            throw new GenericException("User is not in pending status.", HttpStatus.BAD_REQUEST);
        }

        // Check if the RFID is unique and not already assigned
        if (userInfo.getRfid() != null && userRepository.existsByRfid(userInfo.getRfid())) {
            throw new GenericException("RFID already assigned to another user.", HttpStatus.BAD_REQUEST);
        }

        // Save the user information in the UserInfo repository
        UserInfo savedUserInfo = userInfoRepository.save(convertToUserInfoEntity(user, userInfo));

        if (savedUserInfo.getId() == null) {
            throw new GenericException("Failed to save user information.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Update and approve the user
        user.setStatus(UserStatus.APPROVED);
        user.setRfid(userInfo.getRfid()); // Assign RFID during approval
        userRepository.save(user);

        // Map the employee to a manager
        EmployeeManager employeeManager = new EmployeeManager();
        employeeManager.setEmployeeId(user.getId());
        employeeManager.setManagerId(userInfo.getManagerId());
        employeeManagerRepository.save(employeeManager);

        // Return the updated user as a UserDTO
        return new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus());
    }


    // Method for admin to reject a user
    public void rejectUser(Integer userId) {
        // Fetch the user by ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GenericException("User not found with id: " + userId, HttpStatus.NOT_FOUND));

        // Check if the user is in PENDING status
        if (user.getStatus() != UserStatus.PENDING) {
            throw new GenericException("User is not in pending status. Only users with PENDING status can be rejected.", HttpStatus.BAD_REQUEST);
        }

        // Set the user's status to REJECTED and save
        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);
    }


    private UserInfo convertToUserInfoEntity(User user, UserRequestDTO request) {
        UserInfo info = new UserInfo();
        info.setAddress(request.getAddress());
        info.setGender(request.getGender());
        info.setDateOfBirth(request.getDateOfBirth());
        info.setSalary(request.getSalary());
        info.setHireDate(request.getHireDate());
        info.setContact(request.getContact());
        info.setUser(user);
        return info;
    }

}
