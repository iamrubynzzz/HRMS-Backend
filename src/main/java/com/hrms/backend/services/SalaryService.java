package com.hrms.backend.services;

import com.hrms.backend.entities.User;

public interface SalaryService {
    // Calculate salary for all users
    void calculateMonthlySalary(User employee);

    void calculateSalaryForAllEmployees();
}
