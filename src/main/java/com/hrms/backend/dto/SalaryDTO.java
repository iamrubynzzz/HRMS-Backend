package com.hrms.backend.dto;

import com.hrms.backend.entities.SalaryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Data
@AllArgsConstructor
public class SalaryDTO {
    private Long id;
    private String employeeName;
    private double grossSalary;
    private double taxDeduction;
    private float overtimePayTotal;
    private float allowanceAmountTotal;
    private double netSalary;
    private LocalDate calculationDate;
    private SalaryStatus status;
}

