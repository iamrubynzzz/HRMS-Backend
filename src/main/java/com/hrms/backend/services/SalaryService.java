package com.hrms.backend.services;

import com.hrms.backend.entities.Salary;
import com.hrms.backend.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface SalaryService {
    // Calculate salary for all users
    void calculateMonthlySalary(User employee);

    void calculateSalaryForAllEmployees();

    Page<Salary> getSalaries(String employeeName, LocalDate startDate, LocalDate endDate, Pageable pageable);
}
