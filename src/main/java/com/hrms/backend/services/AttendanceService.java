package com.hrms.backend.services;

import com.hrms.backend.dto.AttendanceDTO;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.AttendanceStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceService {
    String clockInOrOut(String rfidTag);
    Optional<Attendance> getAttendanceByUserIdAndDate(int userId, LocalDate date);
    List<AttendanceDTO> getAttendanceByUserIdAndDateRange(int userId, LocalDate startDate, LocalDate endDate);
    void saveAttendance(Attendance attendance);
    AttendanceDTO convertToDTO(Attendance attendance);
    List<AttendanceDTO> getAttendanceByStatusAndDateRange(String status, LocalDate startDate, LocalDate endDate);

    Page<AttendanceDTO> getAllUsersAttendance(String name, LocalDate startDate, LocalDate endDate,String status, int page, int size);

    Page<AttendanceDTO> getEmployeeAttendance(Long employeeId, LocalDate startDate, LocalDate endDate, AttendanceStatus attendanceStatus, int page, int size);

    long countByStatusAndDate(AttendanceStatus attendanceStatus, LocalDate today);
}


