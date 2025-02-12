package com.hrms.backend.services.impl;

import com.hrms.backend.entities.*;
import com.hrms.backend.repository.AttendanceRepository;
import com.hrms.backend.repository.RequestRepository;
import com.hrms.backend.repository.SalaryRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.SalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SalaryServiceImpl implements SalaryService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private SalaryRepository salaryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestRepository requestRepository;

    @Override
    public void calculateMonthlySalary(User employee) {

    }

    @Override
    public void calculateSalaryForAllEmployees() {

    }


    // Tax Calculation Logic

}
