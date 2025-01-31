package com.hrms.backend.services.impl;

import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.Salary;
import com.hrms.backend.entities.User;
import com.hrms.backend.entities.UserInfo;
import com.hrms.backend.repository.AttendanceRepository;
import com.hrms.backend.repository.SalaryRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.SalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SalaryServiceImpl implements SalaryService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private SalaryRepository salaryRepository;

    @Autowired
    private UserRepository userRepository;


    // Tax Calculation Logic
    private double calculateTax(double grossSalary) {
        if (grossSalary <= 10000) {
            return grossSalary * 0.05; // 5% tax for salaries <= 10,000
        } else if (grossSalary <= 20000) {
            return grossSalary * 0.1; // 10% tax for salaries <= 20,000
        } else {
            return grossSalary * 0.2; // 20% tax for salaries > 20,000
        }
    }

    @Override
    public void calculateMonthlySalary() {
       /* List<User> users = userRepository.findAll();

        for (User user : users) {
            // Get user information (salary assigned)
            UserInfo userInfo = user.getUserInfo();

            if (userInfo == null) {
                System.out.println("User info is missing for user: " + user.getId());
                continue;  // Skip this user if userInfo is null
            }

            // Get attendance for the current month
            LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
            List<Attendance> attendances = attendanceRepository.findByUserAndDateBetween(user, startOfMonth, endOfMonth);

            // Calculate present days and total hours worked
            int totalPresentDays = attendances.size();
            int totalWorkedHours = attendances.stream().mapToInt(Attendance::getHoursWorked).sum();

            // Standard working hours (e.g., 8 hours per day)
            int standardWorkingHoursPerDay = 8;
            int totalStandardHours = totalPresentDays * standardWorkingHoursPerDay;

            // Calculate overtime hours
            int overtimeHours = totalWorkedHours > totalStandardHours ? totalWorkedHours - totalStandardHours : 0;

            // Overtime pay rate (e.g., 1.5 times the regular hourly rate)
            double overtimeRate = 1.5;

            //Calculate hourly rate
            double hourlyRate = userInfo.getSalary() / 30 / standardWorkingHoursPerDay;  // Assuming 30 working days in a month

            // Calculate overtime pay
            double overtimePay = overtimeHours * hourlyRate * overtimeRate;

            // Calculate present day salary
            double dailyWage = userInfo.getSalary() / 30;  // Assuming 30 working days in a month
            double presentDaySalary = dailyWage * totalPresentDays;

            // Calculate bonuses (if any)
            double bonus = userInfo.getSalary() * 0.05;  //  5% of monthly salary as bonus

            // Calculate tax deduction
            double taxDeduction = calculateTax(presentDaySalary + overtimePay + bonus);

            // Calculate net salary
            double netSalary = presentDaySalary + overtimePay + bonus - taxDeduction;

            // Save salary record
            Salary salary = new Salary();
            salary.setUser(user);
            salary.setGrossSalary(presentDaySalary + overtimePay + bonus);
            salary.setTaxDeduction(taxDeduction);
            salary.setNetSalary(netSalary);
            salary.setCalculationDate(LocalDate.now());
            salaryRepository.save(salary);

            System.out.println("Salary calculated for: " + user.getName() + " | Net Salary: " + netSalary);
        }*/
    }



    @Override
    public void calculateSalaryForAllEmployees() {
        List<User> employees = userRepository.findAll();
        for (User employee : employees) {
            calculateMonthlySalary();
        }
        System.out.println("Salaries calculated for all employees.");
    }
}
