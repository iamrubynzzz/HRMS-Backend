package com.hrms.backend.controller;

import com.hrms.backend.dto.*;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.Role;
import com.hrms.backend.entities.User;
import com.hrms.backend.repository.AttendanceRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.AttendanceService;
import com.hrms.backend.services.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final AttendanceService attendanceService;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignUpRequest signUpRequest) {
        try {
            Role role = Role.fromString(String.valueOf(signUpRequest.getRole()));

            // Check if the role is Admin, which should not be allowed for signup
            if (role == Role.ADMIN) {
                return ResponseEntity.badRequest().body("Admin role is not allowed for signup.");
            }

            // Set the valid role in the signup request
            signUpRequest.setRole(role);

            // Proceed with signup logic
            User user = authenticationService.signup(signUpRequest);

            // Return success message with user details
            return ResponseEntity.ok("User " + user.getName() + " registered successfully as " + user.getRole().name());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid role provided: " + signUpRequest.getRole());
        }
    }

    // Endpoint for login and JWT generation
    @PostMapping("/login")
    public ResponseEntity<JwtAuthenticationResponse> login(@RequestBody LoginRequest loginRequest) {
        JwtAuthenticationResponse response = authenticationService.login(loginRequest);
        return ResponseEntity.ok(response); // Return JWT and refresh token
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthenticationResponse> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authenticationService.refreshToken(refreshTokenRequest));
    }

    @GetMapping("/error")
    public ResponseEntity<String> handleError() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Authentication failed. Please try again.");
    }



    @PostMapping("/attendance/{rfid}")
    public ResponseEntity<?> clockInOut(@PathVariable String rfid) {
        try {
            // Find the user by RFID
            User user = userRepository.findByRfid(rfid)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with RFID: " + rfid));

            // Get today's date
            LocalDate today = LocalDate.now();

            // Check for existing attendance for today
            Optional<Attendance> existingAttendance = attendanceRepository.findByUserAndDate(user, today);

            if (existingAttendance.isPresent()) {
                Attendance attendance = existingAttendance.get();

                // Update the clock-out time for the day
                attendance.setPunchOut(LocalTime.now());
                attendanceRepository.save(attendance);
                return ResponseEntity.ok("Clock-out successful for " + user.getName());
            } else {
                // No attendance record exists for today, perform clock-in
                Attendance attendance = new Attendance();
                attendance.setUser(user);
                attendance.setName(user.getName()); // Set the user's name
                attendance.setDate(today);
                attendance.setPunchIn(LocalTime.now());
                attendanceRepository.save(attendance);
                return ResponseEntity.ok("Clock-in successful for " + user.getName());
            }
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred: " + e.getMessage());
        }
    }
    @GetMapping("/attendance/{userId}")
    public List<AttendanceDTO> getUserAttendance(@PathVariable int userId) {
        return attendanceService.getAttendanceByUserId(userId);
    }

}
