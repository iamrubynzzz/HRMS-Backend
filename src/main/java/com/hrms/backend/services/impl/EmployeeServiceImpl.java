package com.hrms.backend.services.impl;

import com.hrms.backend.dto.UserRequestDTO;
import com.hrms.backend.dto.UserResponseDTO;
import com.hrms.backend.entities.*;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.exception.ResourceNotFoundException;
import com.hrms.backend.repository.EmployeeManagerRepository;
import com.hrms.backend.repository.UserInfoRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmployeeManagerRepository employeeManagerRepository;


    @Override
    public void createEmployee(UserRequestDTO userRequestDTO, UserInfo userInfo) {
        // Check if managerId is provided
        if (userRequestDTO.getManagerId() == 0) {
            throw new GenericException("Manager ID must be provided to create an employee.", HttpStatus.BAD_REQUEST);
        }

        // Check if the manager exists and is a valid manager
        Optional<User> manager = userRepository.findById(userRequestDTO.getManagerId());
        if (manager.isEmpty() || manager.get().getRole() != Role.MANAGER) {
            throw new GenericException("Manager with the given ID does not exist or is not a valid manager.", HttpStatus.BAD_REQUEST);
        }

        // Save to User table
        User user = new User();
        user.setName(userRequestDTO.getName());
        user.setEmail(userRequestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        user.setRole(Role.EMPLOYEE);
        user.setStatus(UserStatus.APPROVED); // Default status for created employees
        userRepository.save(user);

        // Save to UserInfo table
        userInfo.setUser(user);
        userInfo.setAddress(userRequestDTO.getAddress());
        userInfo.setContact(userRequestDTO.getContact());
        userInfo.setDateOfBirth(userRequestDTO.getDateOfBirth());
        userInfo.setGender(userRequestDTO.getGender());
        userInfo.setHireDate(userRequestDTO.getHireDate());
        userInfo.setSalary(userRequestDTO.getSalary());
        userInfoRepository.save(userInfo);

        // Associate employee with the manager
        EmployeeManager employeeManager = new EmployeeManager();
        employeeManager.setEmployeeId(user.getId());
        employeeManager.setManagerId(userRequestDTO.getManagerId());
        employeeManagerRepository.save(employeeManager);
    }

    @Override
    public UserResponseDTO getEmployeeById(Integer id) {
        // Fetch user details
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " not found"));

        // Fetch user additional info
        UserInfo userInfo = userInfoRepository.findByUserId(user.getId())
                .orElse(null);

        // Fetch managerId from EmployeeManager
        Integer managerId = employeeManagerRepository.findByEmployeeId(id)
                .map(EmployeeManager::getManagerId)
                .orElse(null);

        // Return the response, including managerId
        return new UserResponseDTO(user, userInfo, managerId);
    }


    @Override
    public List<UserResponseDTO> getAllEmployees() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole().name().equalsIgnoreCase("EMPLOYEE"))
                .map(user -> {
                    // Fetch additional information
                    UserInfo userInfo = userInfoRepository.findByUserId(user.getId()).orElse(null);

                    // Fetch managerId from EmployeeManager
                    Integer managerId = employeeManagerRepository.findByEmployeeId(user.getId())
                            .map(EmployeeManager::getManagerId)
                            .orElse(0); // Default to 0 if no managerId is found

                    // Create and return UserResponseDTO
                    return new UserResponseDTO(user, userInfo, managerId);
                })
                .collect(Collectors.toList());
    }


    //Update employee details if needed
    @Override
    public void updateEmployee(Integer id, UserRequestDTO userRequestDTO) {
        // Fetch the user by ID or throw exception if not found
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with ID " + id + " not found"));

        // Update fields only if they are provided
        if (userRequestDTO.getName() != null && !userRequestDTO.getName().isEmpty()) {
            user.setName(userRequestDTO.getName());
        }

        if (userRequestDTO.getEmail() != null && !userRequestDTO.getEmail().isEmpty()
                && !userRequestDTO.getEmail().equals(user.getEmail())) {
            // Check if the email is already in use by another user
            if (userRepository.existsByEmail(userRequestDTO.getEmail())) {
                throw new ResourceNotFoundException("Email " + userRequestDTO.getEmail() + " is already in use.");
            }
            user.setEmail(userRequestDTO.getEmail());
        }

        if (userRequestDTO.getRole() != null) {
            try {
                Role.valueOf(userRequestDTO.getRole().name());
                user.setRole(userRequestDTO.getRole());
            } catch (IllegalArgumentException e) {
                throw new ResourceNotFoundException("Invalid role: " + userRequestDTO.getRole());
            }
        }

        userRepository.save(user);

        // Update the managerId if provided
        if (userRequestDTO.getManagerId() != 0) {
            // Check if the provided managerId exists and has the role of MANAGER
            User manager = userRepository.findById(userRequestDTO.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager with ID " + userRequestDTO.getManagerId() + " not found"));

            if (!manager.getRole().equals(Role.MANAGER)) {
                throw new ResourceNotFoundException("User with ID " + userRequestDTO.getManagerId() + " is not a Manager.");
            }

            // Update employee-manager relationship
            EmployeeManager employeeManager = employeeManagerRepository.findByEmployeeId(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee-Manager relationship for Employee ID " + id + " not found"));
            employeeManager.setManagerId(userRequestDTO.getManagerId());
            employeeManagerRepository.save(employeeManager);
        }

        // Update user additional details if provided
        UserInfo userInfo = userInfoRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User information for ID " + id + " not found"));

        if (userRequestDTO.getAddress() != null && !userRequestDTO.getAddress().isEmpty()) {
            userInfo.setAddress(userRequestDTO.getAddress());
        }

        if (userRequestDTO.getContact() != null && !userRequestDTO.getContact().isEmpty()) {
            userInfo.setContact(userRequestDTO.getContact());
        }

        if (userRequestDTO.getDateOfBirth() != null) {
            userInfo.setDateOfBirth(userRequestDTO.getDateOfBirth());
        }

        if (userRequestDTO.getGender() != null && !userRequestDTO.getGender().isEmpty()) {
            userInfo.setGender(userRequestDTO.getGender());
        }

        userInfoRepository.save(userInfo);
    }

    @Override
    public void deleteEmployee(Integer id) {
        // Fetch the employee by ID or throw an exception if not found
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with ID " + id + " not found"));

        // Delete the employee-manager relationship, if it exists
        try {
            employeeManagerRepository.findByEmployeeId(user.getId())
                    .ifPresent(employeeManager -> employeeManagerRepository.delete(employeeManager));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to delete Employee-Manager relationship for Employee ID: " + id);
        }

        // Delete user information
        try {
            userInfoRepository.deleteByUserId(user.getId());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to delete user information for ID: " + id);
        }

        // Delete the user
        try {
            userRepository.delete(user);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to delete user with ID: " + id);
        }
    }

}
