package com.hrms.backend.controller;

import com.hrms.backend.entities.Attendance;
import com.hrms.backend.services.AttendanceReportService;
import com.hrms.backend.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AttendanceReportController {

    private final AttendanceReportService attendanceReportService;
    private final AttendanceRepository attendanceRepository;

    // Method to generate and download attendance report
    @GetMapping("/generate/attendance/report")
    public ResponseEntity<String> generateAttendanceReport(
            @RequestParam String name,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        try {
            // Fetch attendance records for the given employee and date range
            List<Attendance> attendanceRecords = getAttendanceRecords(name, startDate, endDate);

            // Generate the report and get the file path
            String filePath = attendanceReportService.generateAttendanceReport(attendanceRecords);

            // Respond with the file path or URL where the report is saved
            return ResponseEntity.ok("Attendance report generated successfully! File: " + filePath);
        } catch (JRException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to generate attendance report.");
        }
    }

    // Fetch attendance records based on employeeId and date range from the repository
    private List<Attendance> getAttendanceRecords(String name, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findAttendanceByEmployeeAndDateRange(name, startDate, endDate);
    }
}
