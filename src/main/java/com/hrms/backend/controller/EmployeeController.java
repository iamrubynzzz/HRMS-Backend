package com.hrms.backend.controller;

import com.hrms.backend.dto.UserRequestDTO;
import com.hrms.backend.dto.UserResponseDTO;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.User;
import com.hrms.backend.entities.UserInfo;
import com.hrms.backend.services.EmployeeService;
import com.hrms.backend.services.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class EmployeeController {


    private final EmployeeService employeeService;
    private final UserService userService;
    @GetMapping
    public ResponseEntity<String> sayHello(){
        return ResponseEntity.ok("Hi Employee");
    }


    // Create a new employee
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createUser(@RequestBody UserRequestDTO userRequestDTO) {
        employeeService.createUser(userRequestDTO, new UserInfo());
        return ResponseEntity.ok("User created successfully");
    }


    // Get an employee by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Integer id) {
        UserResponseDTO employee = employeeService.getUserById(id);
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @RequestParam(required = false) String name, // Optional name filter
            @RequestParam(defaultValue = "0") int page,  // Default page number
            @RequestParam(defaultValue = "10") int size  // Default page size
    ) {
        // Fetch paginated and filtered users
        Page<UserResponseDTO> usersPage = employeeService.getAllUsers(name, page, size);

        // Return the response with HTTP 200 OK
        return ResponseEntity.ok(usersPage);
    }

    // Update an existing employee
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateUser(@PathVariable Integer id, @RequestBody UserRequestDTO userRequestDTO) {
        employeeService.updateUser(id, userRequestDTO);
        return ResponseEntity.ok("User information updated successfully");
    }

    // Delete an employee
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Integer id) {
        employeeService.deleteUser(id);
        return ResponseEntity.ok("Employee deleted successfully");
    }

    @GetMapping("/details")
    public Integer getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user is authenticated
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            return user.getId();  // Return the userId
        }

        throw new RuntimeException("User not authenticated");
    }
}
