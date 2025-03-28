package com.hrms.backend.services.impl;


import com.hrms.backend.dto.AttendanceDTO;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.AttendanceStatus;
import com.hrms.backend.repository.AttendanceRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.AttendanceService;
import lombok.RequiredArgsConstructor;
import com.hrms.backend.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    @Override
    public String clockInOrOut(String rfidTag) {
        Optional<User> userOptional = userRepository.findByRfid(rfidTag); // Corrected
        if (userOptional.isEmpty()) {
            return "Invalid RFID tag.";
        }

        User user = userOptional.get();
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByUserAndDate(user, today)
                .orElse(new Attendance(null, user, today, null, null, determineStatus(today)));


        if (attendance.getPunchIn() == null) {
            attendance.setPunchIn(LocalTime.now());
            attendance.setStatus(AttendanceStatus.PRESENT);
        } else if (attendance.getPunchOut() == null) {
            attendance.setPunchOut(LocalTime.now());
        }

        attendanceRepository.save(attendance);
        return "Attendance recorded successfully.";
    }

    @Override
    public Optional<Attendance> getAttendanceByUserIdAndDate(int userId, LocalDate date) {
        return attendanceRepository.findByUserIdAndDate(userId, date);
    }

    // New method for fetching records by user ID and date range
    @Override
    public List<AttendanceDTO> getAttendanceByUserIdAndDateRange(int userId, LocalDate startDate, LocalDate endDate) {
        List<Attendance> attendanceList = attendanceRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        return attendanceList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AttendanceDTO> getAttendanceByStatusAndDateRange(String status, LocalDate startDate, LocalDate endDate) {
        AttendanceStatus attendanceStatus = AttendanceStatus.valueOf(status.toUpperCase()); // Convert string to enum
        return attendanceRepository.findByStatusAndDateBetween(attendanceStatus, startDate, endDate);
    }

    @Override
    public Page<AttendanceDTO> getAllUsersAttendance(
            String name,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);

        // Convert status string to enum safely
        AttendanceStatus attendanceStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                attendanceStatus = AttendanceStatus.valueOf(status.toUpperCase()); // Convert to uppercase to match enum
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid attendance status: " + status);
            }
        }

        // Fetch attendance with filtering by status
        Page<Attendance> attendancePage = attendanceRepository.findAllFiltered(
                name, startDate, endDate, attendanceStatus, pageable);

        // Convert entities to DTOs
        return attendancePage.map(this::convertToDTO);
    }

    @Override
    public Page<AttendanceDTO> getEmployeeAttendance(Long employeeId, LocalDate startDate, LocalDate endDate, AttendanceStatus attendanceStatus, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Call the repository with filters
        Page<Attendance> attendancePage = attendanceRepository.findByEmployeeIdAndFilters(employeeId, startDate, endDate, attendanceStatus, pageable);

        return attendancePage.map(this::convertToDTO);
    }

    public long countByStatusAndDate(AttendanceStatus status, LocalDate date) {
        return attendanceRepository.countByStatusAndDate(status, date);
    }



    private AttendanceStatus determineStatus(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return AttendanceStatus.WEEK_OFF;
        }
        return AttendanceStatus.ABSENT; // Default to absent if no attendance is recorded
    }

    @Override
    public void saveAttendance(Attendance attendance){
        attendanceRepository.save(attendance);
    }

    @Override
    public AttendanceDTO convertToDTO(Attendance attendance) {
        return new AttendanceDTO(
                attendance.getId(),
                attendance.getUser().getName(),
                attendance.getUser().getId(),
                attendance.getDate(),
                attendance.getPunchIn(),
                attendance.getPunchOut(),
                attendance.getStatus()
        );
    }


}
