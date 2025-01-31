package com.hrms.backend.services;

import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.User;
import java.time.LocalDate;

public interface AttendanceService {
    String clockInOrOut(String rfidTag);
}


