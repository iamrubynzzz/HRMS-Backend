package com.hrms.backend.controller;

import com.hrms.backend.dto.UserRequestDTO;
import com.hrms.backend.dto.UserResponseDTO;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.UserInfo;
import com.hrms.backend.services.EmployeeService;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
public class EmployeeController {


    private final EmployeeService employeeService;
    @GetMapping
    public ResponseEntity<String> sayHello(){
        return ResponseEntity.ok("Hi Employee");
    }


    // Create a new employee
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createEmployee(@RequestBody UserRequestDTO userRequestDTO) {
        try {
            employeeService.createEmployee(userRequestDTO, new UserInfo());
            return ResponseEntity.ok("Employee created successfully");
        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token has expired. Please log in again.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error occurred while creating employee: " + e.getMessage());
        }
    }

    // Get an employee by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<UserResponseDTO> getEmployeeById(@PathVariable Integer id) {
        UserResponseDTO employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employee);
    }

    // Get all employees
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<List<UserResponseDTO>> getAllEmployees() {
        List<UserResponseDTO> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }


    // Update an existing employee
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateEmployee(@PathVariable Integer id, @RequestBody UserRequestDTO userRequestDTO) {
        employeeService.updateEmployee(id, userRequestDTO);
        return ResponseEntity.ok("Employee updated successfully");
    }

    // Delete an employee
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteEmployee(@PathVariable Integer id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee deleted successfully");
    }
}
