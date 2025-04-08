package com.hrms.backend.controller;

import com.hrms.backend.dto.AttendanceDTO;
import com.hrms.backend.dto.AttendanceFilterRequest;
import com.hrms.backend.dto.AttendanceRangeRequest;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.AttendanceStatus;
import com.hrms.backend.entities.User;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.repository.AttendanceRepository;
import com.hrms.backend.repository.EmployeeManagerRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.AttendanceService;
import com.hrms.backend.services.RequestService;
import com.hrms.backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;
    private final RequestService requestService;
    private final UserRepository userRepository;
    private  final UserService userService;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeManagerRepository employeeManagerRepository;
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

    // API for employee attendance list
        @GetMapping("/all")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<Page<AttendanceDTO>> getAllUsersAttendance(
                @RequestParam(required = false) String name,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                @RequestParam(required = false) String status,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size) {

            Page<AttendanceDTO> attendancePage = attendanceService.getAllUsersAttendance(name, startDate, endDate, status, page, size);
            return ResponseEntity.ok(attendancePage);
        }


// API for manager view of  attendance
@GetMapping("/manager-attendance")
@PreAuthorize("hasRole('MANAGER')")
public ResponseEntity<Page<AttendanceDTO>> getManagerAndEmployeesAttendance(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String name,  // Added 'name' filter parameter
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        Principal principal) {

    // Get Manager's details
    Optional<User> managerOptional = userService.findByUsername(principal.getName());
    if (managerOptional.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    User manager = managerOptional.get();
    AttendanceStatus attendanceStatus = (status != null) ? AttendanceStatus.valueOf(status) : null;

    // Fetch manager's own attendance
    Page<AttendanceDTO> managerAttendance = attendanceService.getEmployeeAttendance(Long.valueOf(manager.getId()), startDate, endDate, attendanceStatus, name, page, size);

    // Fetch assigned employees' attendance
    List<Integer> managedEmployeeIds = employeeManagerRepository.findEmployeeIdsByManagerId(manager.getId());
    Page<AttendanceDTO> employeesAttendance = attendanceService.getEmployeesAttendance(managedEmployeeIds, startDate, endDate, attendanceStatus, name, page, size);

    // Combine both pages into one
    List<AttendanceDTO> combinedContent = new ArrayList<>();
    combinedContent.addAll(managerAttendance.getContent());
    combinedContent.addAll(employeesAttendance.getContent());

    // Create a new Page with combined content
    Page<AttendanceDTO> combinedPage = new PageImpl<>(combinedContent,
            PageRequest.of(page, size),
            managerAttendance.getTotalElements() + employeesAttendance.getTotalElements());

    return ResponseEntity.ok(combinedPage);
}

    // API for employee to view their own attendance
    @GetMapping("/my-attendance")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<AttendanceDTO>> getEmployeeOwnAttendance(

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {

        // Get employee by username
        Optional<User> userOptional = userService.findByUsername(principal.getName());
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        User employee = userOptional.get();
        AttendanceStatus attendanceStatus = (status != null) ? AttendanceStatus.valueOf(status) : null;

        // Fetch only the logged-in employee's attendance
        Page<AttendanceDTO> attendancePage = attendanceService.getEmployeeAttendance(
                Long.valueOf(employee.getId()),
                startDate,
                endDate,
                attendanceStatus,
                name, // name filter not needed here
                page,
                size
        );

        return ResponseEntity.ok(attendancePage);
    }



    // To show attendance in pie-chart
    @GetMapping("/stats/today")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getAttendanceStatsForToday() {
        LocalDate today = LocalDate.now();

        long presentCount = attendanceService.countByStatusAndDate(AttendanceStatus.PRESENT, today);
        long absentCount = attendanceService.countByStatusAndDate(AttendanceStatus.ABSENT, today);

        // Combine all leave types
        long leaveCount = attendanceService.countByStatusAndDate(AttendanceStatus.ANNUAL_LEAVE, today) +
                attendanceService.countByStatusAndDate(AttendanceStatus.SICK_LEAVE, today) +
                attendanceService.countByStatusAndDate(AttendanceStatus.UNPAID_LEAVE, today);

        Map<String, Long> stats = new HashMap<>();
        stats.put("Present", presentCount);
        stats.put("Absent", absentCount);
        stats.put("Leave", leaveCount);

        return ResponseEntity.ok(stats);
    }
}
