package com.hrms.backend.dto;

import com.hrms.backend.entities.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceDTO {
    private Integer id;
    private LocalDate date;
    private LocalTime punchIn;
    private LocalTime punchOut;
    private String status;



    public AttendanceDTO(Long id, int userId, LocalDate date, LocalTime punchIn, LocalTime punchOut, AttendanceStatus status) {
        this.id = Math.toIntExact(id);
        this.id = userId;
        this.date = date;
        this.punchIn = punchIn;
        this.punchOut = punchOut;
        this.status = String.valueOf(status);
    }
}
