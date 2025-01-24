package com.hrms.backend.services.impl;

import com.hrms.backend.dto.AttendanceDTO;
import com.hrms.backend.dto.UserDTO;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.User;
import com.hrms.backend.repository.AttendanceRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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
    public List<AttendanceDTO> getAttendanceByUserId(int userId) {
        // Check if the user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User with ID " + userId + " not found."));
        List<Attendance> attendances = attendanceRepository.findByUserId(userId);
        return attendances.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // RFID-based clock-in or clock-out functionality
    @Override
    public String clockInOrOut(String rfid) {
        // Find the user by RFID
        User user = userRepository.findByRfid(rfid)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with RFID: " + rfid));

        // Get today's date
        LocalDate today = LocalDate.now();

        // Check for existing attendance record for today
        Optional<Attendance> existingAttendance = attendanceRepository.findByUserAndDate(user, today);

        if (existingAttendance.isPresent()) {
            // Attendance record exists for today
            Attendance attendance = existingAttendance.get();

            if (attendance.getPunchIn() != null && attendance.getPunchOut() == null) {
                // If punchIn exists but no punchOut, perform clock-out
                attendance.setPunchOut(LocalTime.now());
                attendanceRepository.save(attendance);
                return "Clock-out successful for " + user.getName();
            } else {
                // If both punchIn and punchOut exist, update punchOut
                attendance.setPunchOut(LocalTime.now());
                attendanceRepository.save(attendance);
                return "Clock-out time updated for " + user.getName();
            }
        } else {
            // No attendance record exists for today, perform clock-in
            Attendance attendance = new Attendance();
            attendance.setUser(user);
            attendance.setName(user.getName());
            attendance.setDate(today);
            attendance.setPunchIn(LocalTime.now());
            attendanceRepository.save(attendance);
            return "Clock-in successful for " + user.getName();
        }
    }



    // Helper method to map Attendance entity to DTO
    private AttendanceDTO mapToDTO(Attendance attendance) {
        UserDTO userDTO = new UserDTO(
                attendance.getUser().getId(),
                attendance.getUser().getName(),
                attendance.getUser().getEmail()
        );
        return new AttendanceDTO(
                attendance.getId(),
                attendance.getName(),
                userDTO,
                attendance.getPunchIn(),
                attendance.getPunchOut(),
                attendance.getDate()
        );
    }
}
