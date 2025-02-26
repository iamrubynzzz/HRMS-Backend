package com.hrms.backend.scheduler;

import com.hrms.backend.dto.UserDTO;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.AttendanceStatus;
import com.hrms.backend.entities.Status;
import com.hrms.backend.services.AttendanceService;
import com.hrms.backend.services.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DailyAttendanceManagingScheduler {
    private final AttendanceService attendanceService;

    private final UserServiceImpl userService;

 //   @Scheduled(cron = "0 * * * * ?") // run scheduler every minute to test
    @Scheduled(cron = "0 0 1 * * ?") // run scheduler at 1:00 am in the morning
    public void manageDailyAttendance() {
        List<UserDTO> users = userService.getUsersByStatus(Status.APPROVED);
        for (int i = 0; i < users.size(); i++) {
            manageAttendance(users.get(i).getId());
        }
    }

    private void manageAttendance(int userId){
        Optional< Attendance> attendanceOptional = attendanceService.getAttendanceByUserIdAndDate(userId, LocalDate.now().minusDays(1));
        if(attendanceOptional.isPresent()){
            Attendance attendance = attendanceOptional.get();
            if(attendance.getPunchOut() == null){
                attendance.setPunchOut(LocalTime.from(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(18,0))));
            }
            attendance.setStatus(AttendanceStatus.PRESENT);
            attendanceService.saveAttendance(attendance);
        }else{
            Attendance attendance = new Attendance();
            attendance.setUser(userService.getUserById(userId));
            if((LocalDate.now().minusDays(1).getDayOfWeek() == DayOfWeek.SATURDAY)){
                attendance.setStatus(AttendanceStatus.WEEK_OFF);
            }else{
                attendance.setStatus(AttendanceStatus.ABSENT);
            }
            attendanceService.saveAttendance(attendance);
        }
    }
}
