package com.hrms.backend.services.impl;

import com.hrms.backend.dto.UserRequestDTO;
import com.hrms.backend.dto.UserResponseDTO;
import com.hrms.backend.entities.*;
import com.hrms.backend.exception.DeletionException;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.exception.ResourceNotFoundException;
import com.hrms.backend.repository.*;
import com.hrms.backend.services.EmailService;
import com.hrms.backend.services.EmployeeService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


import java.security.SecureRandom;
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
    private final CompanyRepository companyRepository;
    private final HttpSession session;
    private final EmailService emailService;
    private final EmailMessageRepository emailMessageRepository;

    @Override
    public void createUser(UserRequestDTO userRequestDTO, UserInfo userInfo) {

        // Check if role is valid
        if (!userRequestDTO.getRole().equals(Role.ADMIN) && !userRequestDTO.getRole().equals(Role.EMPLOYEE) && !userRequestDTO.getRole().equals(Role.MANAGER)) {
            throw new GenericException("Invalid role. Only ADMIN, EMPLOYEE, and MANAGER roles are allowed.", HttpStatus.FORBIDDEN);
        }

        // Get the authenticated requester's details
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requesterEmail = authentication.getName(); // Get the email of the logged-in user
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new GenericException("Requester not found.", HttpStatus.NOT_FOUND));

        Company company = requester.getCompany();
        Role requesterRole = requester.getRole();

        // Super Admin can only create Admins and must provide companyId explicitly
        if (requesterRole == Role.SUPER_ADMIN) {
            if (userRequestDTO.getRole() != Role.ADMIN) {
                throw new GenericException("Super Admin can only create Admins.", HttpStatus.FORBIDDEN);
            }

            if (userRequestDTO.getCompanyId() == null) {
                throw new GenericException("Company ID must be provided when creating an Admin.", HttpStatus.FORBIDDEN);
            }

            company = companyRepository.findById(Long.valueOf(userRequestDTO.getCompanyId()))
                    .orElseThrow(() -> new GenericException("Company not found with ID: " + userRequestDTO.getCompanyId(), HttpStatus.NOT_FOUND));
        }

        // Admins can only create Managers and Employees for their own company
        if (requesterRole == Role.ADMIN && (userRequestDTO.getRole() == Role.ADMIN || userRequestDTO.getRole() == Role.SUPER_ADMIN)) {
            throw new GenericException("Admins can only create Managers and Employees.", HttpStatus.FORBIDDEN);
        }

        // If the requester is an Admin or Manager, ensure the user is created for their own company
        if (requesterRole == Role.ADMIN || requesterRole == Role.MANAGER) {
            if (company == null) {
                throw new GenericException("Requester must be associated with a company.", HttpStatus.FORBIDDEN);
            }
            userRequestDTO.setCompanyId(Integer.valueOf(company.getId().toString()));
        } else if (requesterRole != Role.SUPER_ADMIN) {
            if (company == null) {
                throw new GenericException("Company must be provided for Admin, Manager, and Employee.", HttpStatus.FORBIDDEN);
            }
            company = companyRepository.findById(Long.valueOf(userRequestDTO.getCompanyId())).get();
        }

        // Check if RFID is unique and not already assigned
        if (userRequestDTO.getRfid() != null && userRepository.existsByRfid(userRequestDTO.getRfid())) {
            throw new GenericException("RFID already assigned to another user.", HttpStatus.CONFLICT);
        }

        if (company == null) {
            throw new GenericException("Please select Company for the user!", HttpStatus.FORBIDDEN);
        }

        // Ensure leave balances are provided for Employees and Managers (not for Admin)
        if (userRequestDTO.getRole() == Role.EMPLOYEE || userRequestDTO.getRole() == Role.MANAGER) {
            if (Objects.isNull(userRequestDTO.getAnnualLeaveBalance())) {
                throw new GenericException("Annual Leave Balance must be provided.", HttpStatus.BAD_REQUEST);
            }
            if (Objects.isNull(userRequestDTO.getSickLeaveBalance())) {
                throw new GenericException("Sick Leave Balance must be provided.", HttpStatus.BAD_REQUEST);
            }
        }

        // Admins can have null leave balances
        if (userRequestDTO.getRole() == Role.ADMIN) {
            userRequestDTO.setAnnualLeaveBalance(null);
            userRequestDTO.setSickLeaveBalance(null);
        }

        // Validate manager ID based on the user's role
        Integer managerIdToAssign = null; // Variable to store the manager ID to assign

        if (userRequestDTO.getRole() == Role.MANAGER) {
            // Automatically find an ADMIN to assign as the manager for the user whose role is manager
            User adminManager = userRepository.findFirstByRoleAndCompany(Role.ADMIN, company)
                    .orElseThrow(() -> new GenericException("No ADMIN found to assign as manager.", HttpStatus.NOT_FOUND));

            managerIdToAssign = adminManager.getId(); // Assign admin as manager

        } else if (userRequestDTO.getRole() == Role.EMPLOYEE && Objects.nonNull(userRequestDTO.getManagerId())) {
            // For Employee roles, validate that the manager exists and is a MANAGER
            User manager = userRepository.findById(userRequestDTO.getManagerId())
                    .orElseThrow(() -> new GenericException("Manager not found with ID: " + userRequestDTO.getManagerId(), HttpStatus.NOT_FOUND));

            if (manager.getRole() != Role.MANAGER) {
                throw new GenericException("The provided Manager ID does not belong to a valid manager.", HttpStatus.BAD_REQUEST);
            }

            managerIdToAssign = userRequestDTO.getManagerId(); // Assign provided manager ID
        }

        String generatedPassword = generateRandomString(5);
        // Save to User table
        User user = new User();
        user.setName(userRequestDTO.getName());
        user.setEmail(userRequestDTO.getEmail());
        user.setPassword(passwordEncoder.encode(generatedPassword));
        user.setRole(userRequestDTO.getRole());
        user.setRfid(userRequestDTO.getRfid());
        user.setStatus(Status.APPROVED); // Default status for created users
        user.setCompany(company);
        try {
            userRepository.save(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("--------------------" + user.getId());
        if (user.getId() != null) {
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

            // Send Email
            String recipientEmail = user.getEmail();
            String subject = "Welcome to HRMS";
            String emailContent = "<h1>Welcome " + user.getName() + "!</h1>" +
                    "<p>Your account has been successfully created in <strong>" + company.getName() + "</strong>.</p><br/>" +
                    "<p><strong>Company Name:</strong> " + company.getName() + "</p>" +
                    "<p><strong>Username:</strong> " + user.getEmail() + "</p>" +
                    "<p><strong>Password:</strong> " + generatedPassword + "</p>";

            // Save Email Message to DB
            EmailMessage emailMessage = new EmailMessage();
            emailMessage.setRecipientAddress(recipientEmail);
            emailMessage.setSubject(subject);
            emailMessage.setMessage(emailContent);
            emailMessage.setCompanyName(company.getName());
            emailMessage.setStatus(EmailStatus.PENDING);
            emailMessageRepository.save(emailMessage);

        }

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
                throw new GenericException("Email " + userRequestDTO.getEmail() + " is already in use.", HttpStatus.CONFLICT);
            }
            user.setEmail(userRequestDTO.getEmail());
        }

        if (userRequestDTO.getRole() != null) {
            try {
                Role.valueOf(userRequestDTO.getRole().name());
                user.setRole(userRequestDTO.getRole());
            } catch (IllegalArgumentException e) {
                throw new GenericException("Invalid role: " + userRequestDTO.getRole(), HttpStatus.BAD_REQUEST);
            }
        }

        userRepository.save(user);

        // Update the managerId if provided
        if (userRequestDTO.getManagerId() != 0) {
            // Check if the provided managerId exists and has the role of MANAGER
            User manager = userRepository.findById(userRequestDTO.getManagerId())
                    .orElseThrow(() -> new GenericException("Manager with ID " + userRequestDTO.getManagerId() + " not found", HttpStatus.NOT_FOUND));

            if (!manager.getRole().equals(Role.MANAGER)) {
                throw new GenericException("User with ID " + userRequestDTO.getManagerId() + " is not a Manager.", HttpStatus.BAD_REQUEST);
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

        // Update Leave Balances
        if (userRequestDTO.getAnnualLeaveBalance() != null) {
            userInfo.setAnnualLeaveBalance(userRequestDTO.getAnnualLeaveBalance());
        }

        if (userRequestDTO.getSickLeaveBalance() != null) {
            userInfo.setSickLeaveBalance(userRequestDTO.getSickLeaveBalance());
        }


        userInfoRepository.save(userInfo);
    }

    @Transactional
    @Override
    public void deleteUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User  with ID " + id + " not found"));

        try {
            // Delete dependent records if they exist
            attendanceRepository.deleteByUser(user);
            salaryRepository.deleteByUser(user);
            requestRepository.deleteByUser(user);

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

    public String generateRandomString(int length) {
        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom RANDOM = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
