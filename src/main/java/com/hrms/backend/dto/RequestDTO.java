package com.hrms.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RequestDTO {
    private Long id;
    private Long userId;
    private String requestType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String status;
    private Float allowanceAmount;
    private Integer overtimeHours;
}
