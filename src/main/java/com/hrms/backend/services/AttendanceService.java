package com.hrms.backend.services;

import com.hrms.backend.dto.AttendanceDTO;
import com.hrms.backend.entities.Attendance;

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

}


