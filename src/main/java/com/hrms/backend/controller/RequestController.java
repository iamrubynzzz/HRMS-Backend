package com.hrms.backend.controller;

import com.hrms.backend.dto.RequestDTO;
import com.hrms.backend.entities.Request;
import com.hrms.backend.entities.Status;
import com.hrms.backend.entities.User;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.JWTService;
import com.hrms.backend.services.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import com.hrms.backend.services.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/requests")
public class RequestController {

    @Autowired
    private RequestService requestService;

    @Autowired
    private UserRepository userRepository;
    private final UserService userService;

    @Qualifier("JWTServiceImpl")
    private final JWTService jwtService;

    @PostMapping
    public ResponseEntity<RequestDTO> createRequest(@RequestBody RequestDTO requestDTO) {
        return ResponseEntity.ok(requestService.createRequest(requestDTO));
    }

    @GetMapping
    public ResponseEntity<List<RequestDTO>> getAllRequests() {
        return ResponseEntity.ok(requestService.getAllRequests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestDTO> getRequestById(@PathVariable Long id) {
        Optional<RequestDTO> requestDTO = requestService.getRequestById(id);
        return requestDTO.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RequestDTO>> getRequestsByUser(@PathVariable Long userId) {
        // Fetch the user from the database
        User user = userRepository.findById(Math.toIntExact(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Return the list of requests for the found user
        return ResponseEntity.ok(requestService.getRequestsByUser(user));
    }

    @GetMapping("/list/{createdDate}")
    public ResponseEntity<List<RequestDTO>> getRequestsByCreatedDate(@PathVariable LocalDate createdDate) {
        return ResponseEntity.ok(requestService.getRequestsByCreatedDate(createdDate));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Request> updateRequestStatus(@PathVariable Long id, @RequestParam Status status) {
        Request updatedRequest = requestService.updateRequestStatus(id, status);
        if (updatedRequest != null) {
            return ResponseEntity.ok(updatedRequest);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/approve/{requestId}")
    public ResponseEntity<RequestDTO> approveRequest(
            @PathVariable Long requestId,
            @RequestHeader("Authorization") String token) {
        try {
            // Verify the approver using the token
            String email = jwtService.extractUsername(token.substring(7));
            User user = userService.getUserByEmail(email);
            // Call the service to approve or reject the leave request and update the leave balance
            RequestDTO updatedRequest = requestService.approveRequest(requestId, user.getId());

            return new ResponseEntity<>(updatedRequest, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/reject/{requestId}")
    public ResponseEntity<RequestDTO> rejectRequest(
            @PathVariable Long requestId,
            @RequestHeader("Authorization") String token) {
        try {
            // Verify the approver using the token
            String email = jwtService.extractUsername(token.substring(7));
            User user = userService.getUserByEmail(email);
            // Call the service to approve or reject the leave request and update the leave balance
            RequestDTO updatedRequest = requestService.rejectRequest(requestId, user.getId());  // Use the approver's ID from the user table

            return new ResponseEntity<>(updatedRequest, HttpStatus.OK);
        } catch (RuntimeException e) {
            // Return error response in case of failure (e.g., insufficient leave balance, unauthorized approver)
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

}
