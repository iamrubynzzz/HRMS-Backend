package com.hrms.backend.controller;

import com.hrms.backend.dto.RequestDTO;
import com.hrms.backend.entities.*;
import com.hrms.backend.exception.AccessDeniedException;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.JWTService;
import com.hrms.backend.services.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import com.hrms.backend.services.UserService;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
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

    @GetMapping("/my-requests")
    public ResponseEntity<Page<RequestDTO>> getMyRequests(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate,desc") String sort,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        String username = principal.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Parse sort parameter
        String[] sortParams = sort.split(",");
        Sort.Direction direction = Sort.Direction.fromString(sortParams[1]);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        Page<RequestDTO> myRequests = requestService.getAllRequestsForLoggedInUser(user, status, date, pageable);
        return ResponseEntity.ok(myRequests);
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(@AuthenticationPrincipal User user) {
        Map<String, Object> response = new HashMap<>();

        // User details
        response.put("fullName", user.getName());
        response.put("initials", getUserInitials(user.getName()));
        response.put("email", user.getEmail());

        // Fetch UserInfo if available
        UserInfo userInfo = user.getUserInfo();
        if (userInfo != null) {
            response.put("address", userInfo.getAddress());
            response.put("contact", userInfo.getContact());
            response.put("dateOfBirth", userInfo.getDateOfBirth());
            response.put("gender", userInfo.getGender());
            response.put("hireDate", userInfo.getHireDate());
            response.put("userId", user.getId());
        } else {
            response.put("address", null);
            response.put("contact", null);
            response.put("dateOfBirth", null);
            response.put("gender", null);
            response.put("hireDate", null);
        }

        return ResponseEntity.ok(response);
    }

    public String getUserInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "?";
        }

        String[] names = fullName.trim().split("\\s+");
        if (names.length == 1) {
            return names[0].substring(0, 1).toUpperCase();
        }
        return (names[0].substring(0, 1) + names[names.length - 1].substring(0, 1)).toUpperCase();
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

    @DeleteMapping("/{requestId}/cancel")
    public ResponseEntity<String> cancelRequest(@PathVariable Long requestId, Principal principal) {
        String username = principal.getName();
        boolean isCancelled = requestService.cancelRequest(requestId, username);

        if (isCancelled) {
            return ResponseEntity.ok("Request cancelled successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request cancellation failed.");
        }
    }

    @GetMapping("/all-requests")
    public ResponseEntity<Page<RequestDTO>> getAllRequestsForAdmin(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate,desc") String sort,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String employeeName
    ) {

        // Get logged-in user
        String username = principal.getName();
        System.out.println("============"+username);
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        System.out.println("User roles: " + user.getAuthorities());

        // Ensure the user is an ADMIN
        if (!user.getAuthorities().contains(new SimpleGrantedAuthority(Role.ADMIN.name()))) {
            throw new AccessDeniedException("Only admins can view all requests.", HttpStatus.FORBIDDEN);
        }

        // Parse sort parameter
        String[] sortParams = sort.split(",");
        Sort.Direction direction = Sort.Direction.fromString(sortParams[1]);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        Page<RequestDTO> allRequests = requestService.getAllRequests(user, status, date, employeeName, pageable);
        return ResponseEntity.ok(allRequests);
    }
}
