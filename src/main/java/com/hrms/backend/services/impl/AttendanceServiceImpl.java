package com.hrms.backend.services.impl;


import com.hrms.backend.dto.AttendanceDTO;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.AttendanceStatus;
import com.hrms.backend.repository.AttendanceRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.AttendanceService;
import lombok.RequiredArgsConstructor;
import com.hrms.backend.entities.User;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

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
                attendance.getUser().getId(),
                attendance.getDate(),
                attendance.getPunchIn(),
                attendance.getPunchOut(),
                attendance.getStatus()
        );
    }
}
