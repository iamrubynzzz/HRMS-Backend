package com.hrms.backend.controller;

import com.hrms.backend.dto.AttendanceDTO;
import com.hrms.backend.dto.AttendanceFilterRequest;
import com.hrms.backend.dto.AttendanceRangeRequest;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.User;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.AttendanceService;
import com.hrms.backend.services.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
public class AttendanceController {
    private final AttendanceService attendanceService;
    private final RequestService requestService;
    private final UserRepository userRepository;
    @PostMapping("/{rfid}")
    public ResponseEntity<?> clockInOut(@PathVariable String rfid) {
        try {
            String result = attendanceService.clockInOrOut(rfid);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/{date}")
    public ResponseEntity<?> getAttendance(@PathVariable int userId, @PathVariable String date) {
        LocalDate attendanceDate = LocalDate.parse(date);

        Optional<Attendance> attendanceOptional = attendanceService.getAttendanceByUserIdAndDate(userId, attendanceDate);

        if (attendanceOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No attendance record found for user ID " + userId + " on " + attendanceDate);
        }

        AttendanceDTO attendanceDTO = attendanceService.convertToDTO(attendanceOptional.get());
        return ResponseEntity.ok(attendanceDTO);
    }

    // New endpoint for fetching by user ID and date range
    @PostMapping("/{userId}/range")
    public ResponseEntity<?> getAttendanceByRange(
            @PathVariable int userId,
            @RequestBody AttendanceRangeRequest request) {

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        List<AttendanceDTO> attendanceDTOs = attendanceService.getAttendanceByUserIdAndDateRange(userId, start, end);

        if (attendanceDTOs.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No attendance records found for user ID " + userId + " between " + start + " and " + end);
        }

        return ResponseEntity.ok(attendanceDTOs);
    }

    @PostMapping("/filter")
    public ResponseEntity<?> getAttendanceByStatusAndDate(
            @RequestBody AttendanceFilterRequest request) {

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        String status = request.getStatus();

        List<AttendanceDTO> filteredAttendance = attendanceService.getAttendanceByStatusAndDateRange(status, startDate, endDate);

        if (filteredAttendance.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No attendance records found for status '" + status + "' between " + startDate + " and " + endDate);
        }

        return ResponseEntity.ok(filteredAttendance);
    }

    @GetMapping("/check-rfid")
    public ResponseEntity<Map<String, Boolean>> checkRfid(@RequestParam String rfid) {
        boolean exists = userRepository.existsByRfid(rfid);
        return ResponseEntity.ok(Collections.singletonMap("exists", exists));
    }
}
