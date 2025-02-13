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


    // Tax Calculation Logic
    private double calculateTax(double grossSalary) {
        if (grossSalary <= 50000) {
            return grossSalary * 0.05; // 5% tax for salaries <= 10,000
        } else if (grossSalary <= 100000) {
            return grossSalary * 0.1; // 10% tax for salaries <= 20,000
        } else {
            return grossSalary * 0.2; // 20% tax for salaries > 20,000
        }
    }

    @Override
    public void calculateMonthlySalary(User employee) {
        // Get user information (salary assigned)
        UserInfo userInfo = employee.getUserInfo();

        if (userInfo == null) {
            System.out.println("User info is missing for user: " + employee.getId());
            return;  // Skip this user if userInfo is null
        }

        Optional<Salary> employeesSalaryForMonth = salaryRepository.findByUserIdAndCalculationDate(employee.getId(), LocalDate.now());

        if (employeesSalaryForMonth.isPresent()) {
            System.out.println("Salary Already Calculated for user: "+employee.getId());
            return;
        }

        int standardWorkingHoursPerDay = 8;
        int workingHoursInAMonth = getWorkingDaysOfMonth(LocalDate.now().getYear(), LocalDate.now().getMonthValue()) * standardWorkingHoursPerDay;
        int unpaidDaysTotal = getUnpaidLeaveDays(employee);
        int unpaidHours = unpaidDaysTotal * standardWorkingHoursPerDay;
        int paidHours = workingHoursInAMonth - unpaidHours;

        // Calculate overtime hours
        int overtimeHours = getOverTimeHours(employee);

        double totalAllowanceAmount = getTotalAllowance(employee);
        // Overtime pay rate (e.g., 1.5 times the regular hourly rate)
        double overtimeRate = 1.5;

        //Calculate hourly rate
        double hourlyRate = userInfo.getSalary() / workingHoursInAMonth;  // Assuming 30 working days in a month

        // Calculate overtime pay
        double overtimePay = overtimeHours * hourlyRate * overtimeRate;

        // Calculate present day salary
        double presentDaySalary = hourlyRate * paidHours;

        // Calculate tax deduction
        double taxDeduction = calculateTax(presentDaySalary + overtimePay);

        // Calculate net salary
        double netSalary = presentDaySalary + overtimePay + totalAllowanceAmount - taxDeduction;

        // Save salary record
        Salary salaryEntity = new Salary();
        salaryEntity.setUser(employee);
        salaryEntity.setGrossSalary(presentDaySalary + overtimePay);
        salaryEntity.setTaxDeduction(taxDeduction);
        salaryEntity.setNetSalary(netSalary);
        salaryEntity.setCalculationDate(LocalDate.now());
        salaryRepository.save(salaryEntity);

        System.out.println("Salary calculated for: " + employee.getName() + " | Net Salary: " + netSalary);
    }

    @Override
    public void calculateSalaryForAllEmployees() {
        List<User> employees = userRepository.findByStatus(Status.APPROVED);
        for (User employee : employees) {
            calculateMonthlySalary(employee);
        }
        System.out.println("Salaries calculated for all employees.");
    }

    public static int getWorkingDaysOfMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        int workingDays = 0;

        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            DayOfWeek dayOfWeek = date.getDayOfWeek();

            // Count only Monday to Friday as working days
            if (dayOfWeek != DayOfWeek.SATURDAY) {
                workingDays++;
            }
        }
        return workingDays;
    }

    public int getUnpaidLeaveDays(User user) {
        List<Request> totalUnPaidSickRequest = requestRepository.findByUserAndRequestTypeAndStatus(user, RequestType.UNPAID_SICK_LEAVE, Status.APPROVED);
        List<Request> totalUnPaidAnnualRequest = requestRepository.findByUserAndRequestTypeAndStatus(user, RequestType.UNPAID_ANNUAL_LEAVE, Status.APPROVED);
        AtomicInteger totalUnpaidLeaves = new AtomicInteger();

        totalUnPaidAnnualRequest.forEach(i -> totalUnpaidLeaves.addAndGet(i.getLeaveDays()));
        totalUnPaidSickRequest.forEach(i -> totalUnpaidLeaves.addAndGet(i.getLeaveDays()));

        return totalUnpaidLeaves.get();
    }

    public int getOverTimeHours(User user) {
        List<Request> overTimeRequested = requestRepository.findByUserAndRequestTypeAndStatus(user, RequestType.OVERTIME, Status.APPROVED);
        AtomicInteger totalOvertimeHours = new AtomicInteger();

        overTimeRequested.forEach(i -> totalOvertimeHours.addAndGet(i.getLeaveDays()));

        return totalOvertimeHours.get();
    }

    public float getTotalAllowance(User user) {
        List<Request> allowanceRequested = requestRepository.findByUserAndRequestTypeAndStatus(user, RequestType.ALLOWANCE, Status.APPROVED);
        final float[] totalAllowance = {0.0F};

        allowanceRequested.forEach(i -> {
            totalAllowance[0] = totalAllowance[0] + i.getAllowanceAmount();
        });

        return totalAllowance[0];
    }
}
