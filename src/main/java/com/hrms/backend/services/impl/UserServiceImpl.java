package com.hrms.backend.services.impl;

import com.hrms.backend.dto.UserDTO;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    // Method to process OAuth2User
    @Override
    public String processOAuth2User(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null || name == null) {
            throw new GenericException("OAuth2 user information is incomplete.", HttpStatus.BAD_REQUEST);
        }

        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isEmpty()) {
            throw new GenericException("Your email is not registered. Please contact your Admin.", HttpStatus.UNAUTHORIZED);
        }

        User user = existingUser.get();

        if (user.getStatus() == Status.PENDING) {
            throw new GenericException("Your account is pending approval.", HttpStatus.FORBIDDEN);
        }

        // Generate token without first invalidating (since it's a new login)
        try {
            return jwtService.generateToken(user);
        } catch (Exception e) {
            throw new GenericException("Error occurred while generating JWT token: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Implement the getUsersByStatus method
    @Override
    public List<UserDTO> getUsersByStatus(Status userStatus) {
        List<User> users = userRepository.findByStatus(userStatus);

        // Case where no users are found with the given status
        if (users.isEmpty()) {
            throw new GenericException("No users found with status: " + userStatus, HttpStatus.NOT_FOUND);
        }

        return users.stream()
                .map(user -> new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus()))
                .collect(Collectors.toList());
    }


    // Method for admin to approve a user
    @Override
    public UserDTO approveUser(Integer userId, UserRequestDTO userInfo) {
        // Get the currently authenticated user (approver)
        String loggedInUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User approver = userRepository.findByEmail(loggedInUserEmail)
                .orElseThrow(() -> new GenericException("Approver not found.", HttpStatus.UNAUTHORIZED));

        // Ensure only an ADMIN can approve users
        if (approver.getRole() != Role.ADMIN) {
            throw new GenericException("Only an ADMIN can approve users.", HttpStatus.FORBIDDEN);
        }

        // Fetch the user by ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GenericException("User not found with ID: " + userId, HttpStatus.NOT_FOUND));

        // Check if the user is already approved
        if (user.getStatus() == Status.APPROVED) {
            throw new GenericException("User is already approved.", HttpStatus.BAD_REQUEST);
        }

        // Check if the user is in PENDING status
        if (user.getStatus() != Status.PENDING) {
            throw new GenericException("User is not in pending status.", HttpStatus.NOT_FOUND);
        }

        // Check if the RFID is unique and not already assigned
        if (userInfo.getRfid() != null && userRepository.existsByRfid(userInfo.getRfid())) {
            throw new GenericException("RFID already assigned to another user.", HttpStatus.CONFLICT);
        }

        // Ensure leave balances are provided
        if (Objects.isNull(userInfo.getAnnualLeaveBalance())) {
            throw new GenericException("Annual Leave Balance must be provided.", HttpStatus.BAD_REQUEST);
        }
        if (Objects.isNull(userInfo.getSickLeaveBalance())) {
            throw new GenericException("Sick Leave Balance must be provided.", HttpStatus.BAD_REQUEST);
        }

        Integer managerIdToAssign = null; // Variable to store the manager ID to assign

        // Validate manager ID based on the user's role
        if (user.getRole() == Role.MANAGER) {
            // Automatically find an ADMIN to assign as the manager for the user whose role is manager
            User adminManager = userRepository.findFirstByRoleAndCompany(Role.ADMIN, approver.getCompany())
                    .orElseThrow(() -> new GenericException("No ADMIN found to assign as manager.", HttpStatus.NOT_FOUND));

            managerIdToAssign = adminManager.getId(); // Assign admin as manager

        } else if (Objects.nonNull(userInfo.getManagerId())) {
            // For non-MANAGER roles, validate that the manager exists and is a MANAGER
            User manager = userRepository.findById(userInfo.getManagerId())
                    .orElseThrow(() -> new GenericException("Manager not found with ID: " + userInfo.getManagerId(), HttpStatus.NOT_FOUND));

            if (manager.getRole() != Role.MANAGER) {
                throw new GenericException("The provided Manager ID does not belong to a valid manager.", HttpStatus.BAD_REQUEST);
            }

            // Check if the employee is already assigned to another manager
            if (employeeManagerRepository.existsByEmployeeId(user.getId())) {
                throw new GenericException("Employee is already assigned to another manager.", HttpStatus.CONFLICT);
            }

            managerIdToAssign = userInfo.getManagerId(); // Assign provided manager ID
        }

        // Convert DTO to UserInfo entity
        UserInfo userInfoEntity = convertToUserInfoEntity(user, userInfo);
        userInfoEntity.setAnnualLeaveBalance(userInfo.getAnnualLeaveBalance());
        userInfoEntity.setSickLeaveBalance(userInfo.getSickLeaveBalance());

        // Save user information
        UserInfo savedUserInfo = userInfoRepository.save(userInfoEntity);

        if (savedUserInfo.getId() == null) {
            throw new GenericException("Failed to save user information.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Update and approve the user
        user.setStatus(Status.APPROVED);
        user.setRfid(userInfo.getRfid());
        userRepository.save(user);

        // Assign Employee or Manager to their respective manager (ADMIN for MANAGER, MANAGER for EMPLOYEE)
        if (managerIdToAssign != null) {
            EmployeeManager employeeManager = new EmployeeManager();
            employeeManager.setEmployeeId(user.getId());
            employeeManager.setManagerId(managerIdToAssign);
            employeeManagerRepository.save(employeeManager);
        }

        // Return the updated user as a UserDTO
        return new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus());
    }


    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new GenericException("User not found", HttpStatus.BAD_REQUEST));
    }

    @Override
    public User getUserById(int id) {
        return userRepository.findById(id).get();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByEmail(username);
    }

    @Override
    public List<User> getAllEmployees() {
        return userRepository.findByRoleIn(List.of(Role.ADMIN, Role.MANAGER, Role.EMPLOYEE));
    }

    public void updatePassword(String email, String newPassword) {
        // Retrieve the user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Encrypt the new password using the password encoder
        user.setPassword(passwordEncoder.encode(newPassword));

        // Save the updated user in the database
        userRepository.save(user);
    }


    // Method for admin to reject a user
    public void rejectUser(Integer userId) {
        // Fetch the user by ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GenericException("User not found with id: " + userId, HttpStatus.NOT_FOUND));

        // Check if the user is in PENDING status
        if (user.getStatus() != Status.PENDING) {
            throw new GenericException("User is not in pending status. Only users with PENDING status can be rejected.", HttpStatus.BAD_REQUEST);
        }

        // Set the user's status to REJECTED and save
        user.setStatus(Status.REJECTED);
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
