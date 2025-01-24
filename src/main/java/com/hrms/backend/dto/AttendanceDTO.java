package com.hrms.backend.dto;

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
    private String name;
    private UserDTO user; // Use UserDTO to exclude sensitive fields
    private LocalTime timeIn;
    private LocalTime timeOut;
    private LocalDate date;
}
