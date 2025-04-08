package com.hrms.backend.services;

import com.hrms.backend.dto.AttendanceDTO;
import com.hrms.backend.entities.Attendance;
import net.sf.jasperreports.engine.JRException;

import java.util.List;

public interface AttendanceReportService {
    String generateAttendanceReport(List<Attendance> attendanceRecords) throws JRException;
}
