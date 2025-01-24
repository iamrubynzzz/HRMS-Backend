package com.hrms.backend.services;

import com.hrms.backend.dto.AttendanceDTO;

import java.util.List;

public interface AttendanceService {
    List<AttendanceDTO> getAttendanceByUserId(int userId); // Get attendance records by user ID

    // RFID-based clock-in or clock-out functionality
    String clockInOrOut(String rfid);
}
