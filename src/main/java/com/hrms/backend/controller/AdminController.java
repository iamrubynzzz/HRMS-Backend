package com.hrms.backend.controller;

import com.hrms.backend.dto.UserDTO;
import com.hrms.backend.dto.UserRequestDTO;
import com.hrms.backend.dto.UserResponseDTO;
import com.hrms.backend.entities.User;
import com.hrms.backend.entities.UserInfo;
import com.hrms.backend.entities.UserStatus;
import com.hrms.backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    //Endpoint to get a list of users with pending approval status (excluding password)
    @GetMapping("/pending-approvals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getPendingUsers() {
        List<UserDTO> pendingUsers = userService.getUsersByStatus(UserStatus.PENDING);
        return ResponseEntity.ok(pendingUsers);
    }

    // Endpoint to approve a user and add details
    @PutMapping("/approve/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> approveUser(@PathVariable Integer userId, @RequestBody UserRequestDTO userRequestDTO) {
        UserDTO userDTO = userService.approveUser(userId, userRequestDTO);

        return ResponseEntity.ok("User approved and details added: " + userDTO.getName());
    }

    // Endpoint to reject a user
    @PutMapping("/reject/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rejectUser(@PathVariable Integer userId) {
        userService.rejectUser(userId);
        return ResponseEntity.ok("User rejected successfully");
    }

    // Simple hello endpoint (as you had it originally)
    @GetMapping
    public ResponseEntity<String> sayHello() {
        return ResponseEntity.ok("Hi Admin");
    }
}
