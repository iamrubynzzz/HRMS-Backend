package com.hrms.backend.services;

import com.hrms.backend.dto.SalaryDTO;
import com.hrms.backend.entities.ConsolidatedSalary;
import com.hrms.backend.entities.Salary;
import com.hrms.backend.entities.SalaryStatus;
import com.hrms.backend.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface SalaryService {
    // Calculate salary for all users
    void calculateMonthlySalary(User employee, ConsolidatedSalary consolidatedSalary);

    void calculateSalaryForAllEmployees();

    Page<Salary> getSalaries(String employeeName, LocalDate startDate, LocalDate endDate, Pageable pageable);

    int approveSalariesByConsolidatedSalary(Long consolidatedSalaryId);

    Page<SalaryDTO> getSalariesByConsolidatedSalaryId(Long id, String employeeName, LocalDate startDate, LocalDate endDate, Pageable pageable);

    int releaseSalariesByConsolidatedSalary(Long consolidatedSalaryId);

    Page<SalaryDTO> getMonthlyPayroll(User user, LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<SalaryDTO> getMonthlyPayrollForUser(User manager, LocalDate startDate, LocalDate endDate, int page, int size);

    Page<SalaryDTO> getMonthlyPayrollForMultipleUsers(List<Long> userIds, LocalDate startDate, LocalDate endDate, int page, int size);
}
