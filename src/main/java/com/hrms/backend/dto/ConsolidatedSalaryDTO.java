package com.hrms.backend.dto;

import com.hrms.backend.entities.ConsolidatedSalaryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ConsolidatedSalaryDTO {
    private Long id;
    private LocalDate calculationDate;
    private int totalNumberOfEmployees;
    private BigDecimal totalExpense;
    private ConsolidatedSalaryStatus status;
}

