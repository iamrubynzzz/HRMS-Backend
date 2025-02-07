package com.hrms.backend.services;

import com.hrms.backend.entities.Attendance;

import java.time.LocalDate;
import java.util.Optional;

public interface AttendanceService {
    String clockInOrOut(String rfidTag);
    Optional<Attendance> getAttendanceByUserIdAndDate(int userId, LocalDate date);
    void saveAttendance(Attendance attendance);

}


