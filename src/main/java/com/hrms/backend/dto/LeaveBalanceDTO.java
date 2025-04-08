package com.hrms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaveBalanceDTO {
    private String name;
    private Integer annualLeaveBalance;
    private Integer sickLeaveBalance;
}
