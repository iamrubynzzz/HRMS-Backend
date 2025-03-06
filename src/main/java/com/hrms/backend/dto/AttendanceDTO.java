package com.hrms.backend.dto;

import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceDTO {
    private Integer id;
    private String name;
    private LocalDate date;
    private LocalTime punchIn;
    private LocalTime punchOut;
    private String status;

    public AttendanceDTO(Long id, String name, Integer id1, LocalDate date, LocalTime punchIn, LocalTime punchOut, AttendanceStatus status) {
        this.id = Math.toIntExact(id);
        this.name = name;
        this.date = date;
        this.punchIn = punchIn;
        this.punchOut = punchOut;
        this.status = String.valueOf(status);
    }
}
