package com.hrms.backend.services.impl;

import com.hrms.backend.dto.UserRequestDTO;
import com.hrms.backend.dto.UserResponseDTO;
import com.hrms.backend.entities.*;
import com.hrms.backend.exception.DeletionException;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.exception.ResourceNotFoundException;
import com.hrms.backend.repository.*;
import com.hrms.backend.services.EmployeeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmployeeManagerRepository employeeManagerRepository;
    private final AttendanceRepository attendanceRepository;
    private final SalaryRepository salaryRepository;
    private final RequestRepository requestRepository;
    @Override
    public void createUser(UserRequestDTO userRequestDTO, UserInfo userInfo) {
        // Check if role is valid
        if (!userRequestDTO.getRole().equals(Role.EMPLOYEE) && !userRequestDTO.getRole().equals(Role.MANAGER)) {
            throw new GenericException("Invalid role. Only EMPLOYEE and MANAGER roles are allowed.", HttpStatus.FORBIDDEN);
        }

        // Check if RFID is unique and not already assigned
        if (userRequestDTO.getRfid() != null && userRepository.existsByRfid(userRequestDTO.getRfid())) {
            throw new GenericException("RFID already assigned to another user.", HttpStatus.CONFLICT);
        }

        // Ensure leave balances are provided
        if (Objects.isNull(userRequestDTO.getAnnualLeaveBalance())) {
            throw new GenericException("Annual Leave Balance must be provided.", HttpStatus.BAD_REQUEST);
        }
        if (Objects.isNull(userRequestDTO.getSickLeaveBalance())) {
            throw new GenericException("Sick Leave Balance must be provided.", HttpStatus.BAD_REQUEST);
        }

        // Validate manager ID based on the user's role
        Integer managerIdToAssign = null; // Variable to store the manager ID to assign

        if (userRequestDTO.getRole() == Role.MANAGER) {
            // Automatically find an ADMIN to assign as the manager for the user whose role is manager
            User adminManager = userRepository.findFirstByRole(Role.ADMIN)
                    .orElseThrow(() -> new GenericException("No ADMIN found to assign as manager.", HttpStatus.NOT_FOUND));

            managerIdToAssign = adminManager.getId(); // Assign admin as manager

        } else if (Objects.nonNull(userRequestDTO.getManagerId())) {
            // For non-MANAGER roles, validate that the manager exists and is a MANAGER
            User manager = userRepository.findById(userRequestDTO.getManagerId())
                    .orElseThrow(() -> new GenericException("Manager not found with ID: " + userRequestDTO.getManagerId(), HttpStatus.NOT_FOUND));

            if (manager.getRole() != Role.MANAGER) {
                throw new GenericException("The provided Manager ID does not belong to a valid manager.", HttpStatus.BAD_REQUEST);
            }

            managerIdToAssign = userRequestDTO.getManagerId(); // Assign provided manager ID
        }

        // Save to User table
        User user = new User();
        user.setName(userRequestDTO.getName());
        user.setEmail(userRequestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        user.setRole(userRequestDTO.getRole());
        user.setRfid(userRequestDTO.getRfid());
        user.setStatus(Status.APPROVED); // Default status for created users
        userRepository.save(user);

        // Save to UserInfo table
        userInfo.setUser(user);
        userInfo.setAddress(userRequestDTO.getAddress());
        userInfo.setContact(userRequestDTO.getContact());
        userInfo.setDateOfBirth(userRequestDTO.getDateOfBirth());
        userInfo.setGender(userRequestDTO.getGender());
        userInfo.setHireDate(userRequestDTO.getHireDate());
        userInfo.setSalary(userRequestDTO.getSalary());
        userInfo.setAnnualLeaveBalance(userRequestDTO.getAnnualLeaveBalance());
        userInfo.setSickLeaveBalance(userRequestDTO.getSickLeaveBalance());
        userInfoRepository.save(userInfo);

        // Assign Employee or Manager to their respective manager (ADMIN for MANAGER, MANAGER for EMPLOYEE)
        if (managerIdToAssign != null) {
            EmployeeManager employeeManager = new EmployeeManager();
            employeeManager.setEmployeeId(user.getId());
            employeeManager.setManagerId(managerIdToAssign);
            employeeManagerRepository.save(employeeManager);
        }
    }
    @Override
    public UserResponseDTO getUserById(Integer id) {
        // Fetch user details
        User user = userRepository.findById(id)
                .orElseThrow(() -> new GenericException("User with ID " + id + " not found", HttpStatus.NOT_FOUND));

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
    public Page<UserResponseDTO> getAllUsers(String name, int page, int size) {
        // Create a Pageable object for pagination
        Pageable pageable = PageRequest.of(page, size);

        // Fetch users with pagination and filtering
        Page<User> usersPage = userRepository.findAllFiltered(
                name, // Name filter (can be null)
                Role.EMPLOYEE, // Role filter for EMPLOYEE
                Role.MANAGER, // Role filter for MANAGER
                Status.APPROVED, // Status filter for APPROVED
                pageable
        );

        // Map the results to UserResponseDTO
        return usersPage.map(user -> {
            // Fetch additional information
            UserInfo userInfo = userInfoRepository.findByUserId(user.getId()).orElse(null);

            // Fetch managerId from EmployeeManager
            Integer managerId = employeeManagerRepository.findByEmployeeId(user.getId())
                    .map(EmployeeManager::getManagerId)
                    .orElse(0); // Default to 0 if no managerId is found

            // Create and return UserResponseDTO
            return new UserResponseDTO(user, userInfo, managerId);
        });
    }

    //Update employee details if needed
    @Override
    public void updateUser(Integer id, UserRequestDTO userRequestDTO) {
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

    @Transactional
    @Override
    public void deleteUser (Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User  with ID " + id + " not found"));

        try {
            // Delete dependent records if they exist
            attendanceRepository.deleteByUser (user);
            salaryRepository.deleteByUser (user);
            requestRepository.deleteByUser (user);

            // Delete employee-manager relationship if it exists
            employeeManagerRepository.findByEmployeeId(user.getId())
                    .ifPresent(employeeManagerRepository::delete);

            // Delete user info if it exists
            userInfoRepository.deleteByUserId(user.getId());

            // Finally, delete the user
            userRepository.delete(user);
        } catch (Exception ex) {
            throw new DeletionException("Failed to delete user with ID: " + id + ". Reason: " + ex.getMessage());
        }
    }
}
