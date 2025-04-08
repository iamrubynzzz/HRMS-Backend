package com.hrms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManagerLeaveBalanceResponse {
    private LeaveBalanceDTO managerLeaveBalance;
    private List<LeaveBalanceDTO> employeeLeaveBalances;
}
