package com.hrms.backend.services;

import java.time.LocalDate;

public interface SalaryReportService {
    String generateSalaryReportById(Long salaryId);
}
