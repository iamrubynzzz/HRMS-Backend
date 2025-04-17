package com.hrms.backend.services.impl;

import com.hrms.backend.dto.SalaryDTO;
import com.hrms.backend.entities.*;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.exception.ResourceNotFoundException;
import com.hrms.backend.repository.*;
import com.hrms.backend.services.EmailService;
import com.hrms.backend.services.SalaryService;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    @Autowired
    private ConsolidatedSalaryRepository consolidatedSalaryRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailMessageRepository emailMessageRepository;

    private int numberOfEmployees = 0;
    private BigDecimal totalExpenses = BigDecimal.ZERO;

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
    public void calculateMonthlySalary(User employee, ConsolidatedSalary consolidatedSalary) {
        // Get user information (salary assigned)
        UserInfo userInfo = employee.getUserInfo();

        if (userInfo == null) {
            System.out.println("User info is missing for user: " + employee.getId());
            return;  // Skip this user if userInfo is null
        }

        Optional<Salary> employeesSalaryForMonth = salaryRepository.findByUserIdAndCalculationDate(employee.getId(), LocalDate.now());

        if (employeesSalaryForMonth.isPresent()) {
            System.out.println("Salary Already Calculated for user: " + employee.getId());
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
       // totalExpenses = totalExpenses.add(BigDecimal.valueOf(netSalary));

        // Update total expenses
        consolidatedSalary.setTotalExpense(consolidatedSalary.getTotalExpense().add(BigDecimal.valueOf(netSalary)));
        // Save salary record
        User existingUser = userRepository.findById(employee.getId())
                .orElseThrow(() -> new RuntimeException("User not found")); // Ensure we get a managed entity

        Salary salaryEntity = new Salary();
        salaryEntity.setUser(existingUser); // Now the user is managed
        salaryEntity.setGrossSalary(presentDaySalary + overtimePay);
        salaryEntity.setTaxDeduction(taxDeduction);
        salaryEntity.setNetSalary((float) netSalary);
        salaryEntity.setCalculationDate(LocalDate.now());
        salaryEntity.setStatus(SalaryStatus.PENDING_REVIEW);
        salaryEntity.setConsolidatedSalary(consolidatedSalary);
        salaryRepository.save(salaryEntity);

        System.out.println("Salary calculated for: " + employee.getName() + " | Net Salary: " + netSalary);
    }

    @Override
    public void calculateSalaryForAllEmployees() {
        List<User> employees = userRepository.findByStatus(Status.APPROVED);

        ConsolidatedSalary consolidatedSalary = new ConsolidatedSalary();
        consolidatedSalary.setTotalNumberOfEmployees(0);
        consolidatedSalary.setTotalExpense(BigDecimal.ZERO); // Ensure it has a default value
        consolidatedSalary.setStatus(ConsolidatedSalaryStatus.PENDING_REVIEW);

        consolidatedSalary = consolidatedSalaryRepository.save(consolidatedSalary); // Save to get managed entity

        for (User employee : employees) {
            numberOfEmployees++;
            calculateMonthlySalary(employee, consolidatedSalary);
        }

        // Update total employees and expense before final save
        consolidatedSalary.setTotalNumberOfEmployees(numberOfEmployees);
        consolidatedSalaryRepository.save(consolidatedSalary);

        System.out.println("Salaries calculated for " + numberOfEmployees + " employees.");
    }


    public Page<Salary> getSalaries(String employeeName, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return salaryRepository.findSalaries(employeeName, startDate, endDate, pageable);
    }


    public static int getWorkingDaysOfMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        int workingDays = 0;

        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            DayOfWeek dayOfWeek = date.getDayOfWeek();

            // Count only Sunday to Friday as working days
            if (dayOfWeek != DayOfWeek.SATURDAY) {
                workingDays++;
            }
        }
        return workingDays;
    }


    // Calculate and return total number of unpaid leave days
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

    public Page<SalaryDTO> getSalariesByConsolidatedSalaryId(Long id, String employeeName, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return salaryRepository.findByConsolidatedSalaryIdWithFilters(id, employeeName, startDate, endDate, pageable)
                .map(salary -> new SalaryDTO(
                        salary.getId(),
                        salary.getUser().getName(),
                        salary.getGrossSalary(),
                        salary.getTaxDeduction(),
                        salary.getOvertimePayTotal(),
                        salary.getAllowanceAmountTotal(),
                        salary.getNetSalary(),
                        salary.getCalculationDate(),
                        salary.getStatus()
                ));
    }



    @Transactional
    @Override
    public int approveSalariesByConsolidatedSalary(Long consolidatedSalaryId) {
        // Update the salary status in bulk
        int updatedCount = salaryRepository.bulkApproveSalaries(consolidatedSalaryId);

        // Fetch the ConsolidatedSalary to update its status
        Optional<ConsolidatedSalary> consolidatedSalaryOpt = consolidatedSalaryRepository.findById(consolidatedSalaryId);

        if (consolidatedSalaryOpt.isPresent()) {
            ConsolidatedSalary consolidatedSalary = consolidatedSalaryOpt.get();

            // Update the status of ConsolidatedSalary to APPROVED after bulk salary approval
            consolidatedSalary.setStatus(ConsolidatedSalaryStatus.APPROVED);
            consolidatedSalaryRepository.save(consolidatedSalary); // Save the updated status
        }

        return updatedCount; // Return the number of updated salaries
    }

    @Transactional
    @Override
    public int releaseSalariesByConsolidatedSalary(Long consolidatedSalaryId) {
        // Fetch all salaries related to the ConsolidatedSalary ID
        List<Salary> salaries = salaryRepository.findByConsolidatedSalaryId(consolidatedSalaryId);

        // Iterate over all the salaries and check if they are APPROVED before releasing
        int releasedCount = 0;
        for (Salary salary : salaries) {
            if (salary.getStatus() == SalaryStatus.APPROVED) {
                // Update status to RELEASED
                salary.setStatus(SalaryStatus.RELEASED);
                salaryRepository.save(salary);

                // Send Email Notification
                User user = salary.getUser();
                String recipientEmail = user.getEmail();
                String subject = "Salary Released for " + salary.getCalculationDate();
                String emailContent = "<h1>Salary Details</h1>" +
                        "<p>Dear " + user.getName() + ",</p>" +
                        "<p>Your salary for <strong>" + salary.getCalculationDate() + "</strong> has been released.</p>" +
                        "<p><strong>Gross Salary:</strong> " + salary.getGrossSalary() + "</p>" +
                        "<p><strong>Tax Deduction:</strong> " + salary.getTaxDeduction() + "</p>" +
                        "<p><strong>Net Salary:</strong> " + salary.getNetSalary() + "</p>" +
                        "<br/><p>Regards,<br/>Rubina Thapa</p>";

                // Save Email Message to DB
                EmailMessage emailMessage = new EmailMessage();
                emailMessage.setRecipientAddress(recipientEmail);
                emailMessage.setSubject(subject);
                emailMessage.setMessage(emailContent);
               emailMessage.setCompanyName(user.getCompany().getName());
              //  System.out.println("============="+user.getCompany());
                emailMessage.setStatus(EmailStatus.PENDING);
                emailMessageRepository.save(emailMessage);

                // Try sending the email and catch any MessagingException
                try {
                    emailService.sendEmail(recipientEmail, subject, emailContent);
                } catch (MessagingException e) {
                    System.err.println("Error sending email to " + recipientEmail + ": " + e.getMessage());
                    emailMessage.setStatus(EmailStatus.FAILED);
                    emailMessageRepository.save(emailMessage);
                }

                releasedCount++;
            }
        }

        // After releasing all salaries, update the status of ConsolidatedSalary
        ConsolidatedSalary consolidatedSalary = consolidatedSalaryRepository.findById(consolidatedSalaryId)
                .orElseThrow(() -> new GenericException("ConsolidatedSalary not found", HttpStatus.NOT_FOUND));

        // Update ConsolidatedSalary status to RELEASED
        consolidatedSalary.setStatus(ConsolidatedSalaryStatus.RELEASED);
        consolidatedSalaryRepository.save(consolidatedSalary);

        return releasedCount;
    }

    // Fetch monthly payroll for the manager view with filtering and pagination
    // Fetch payroll for a single user (manager or employee)
    public Page<SalaryDTO> getMonthlyPayrollForUser(User user, LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("calculationDate").descending());
        return salaryRepository.findByUserAndCalculationDateBetween(user, startDate, endDate, pageable)
                .map(this::mapToDTO);  // Map to DTO
    }


    // Method to get payroll for multiple users (employees under a manager)
    public Page<SalaryDTO> getMonthlyPayrollForMultipleUsers(List<Long> userIds, LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("calculationDate").descending());
        return salaryRepository.findByUserIdInAndCalculationDateBetween(userIds, startDate, endDate, pageable)
                .map(this::mapToDTO);  // Mapping the Salary entity to DTO
    }

    // Mapping method from Salary entity to SalaryDTO
    private SalaryDTO mapToDTO(Salary salary) {
        return new SalaryDTO(
                salary.getId(),
                salary.getUser().getName(),
                salary.getGrossSalary(),
                salary.getTaxDeduction(),
                salary.getOvertimePayTotal(),
                salary.getAllowanceAmountTotal(),
                salary.getNetSalary(),
                salary.getCalculationDate(),
                salary.getStatus()
        );
    }

    // Fetch monthly payroll for the logged-in user with filtering and pagination
    @Override
    public Page<SalaryDTO> getMonthlyPayroll(User user, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // Fetch paginated and filtered salary records for the logged-in user
        Page<Salary> salaries;
        if (startDate != null && endDate != null) {
            // Fetch salaries between the start and end date
            salaries = salaryRepository.findByUserAndCalculationDateBetween(user, startDate, endDate, pageable);
        } else {
            // If no date range is specified, fetch all salaries for the user
            salaries = salaryRepository.findByUser(user, pageable);
        }

        // Map the Page<Salary> to Page<SalaryDTO>
        return salaries.map(salary -> new SalaryDTO(
                salary.getId(),
                salary.getUser().getName(),
                salary.getGrossSalary(),
                salary.getTaxDeduction(),
                salary.getOvertimePayTotal(),
                salary.getAllowanceAmountTotal(),
                salary.getNetSalary(),
                salary.getCalculationDate(),
                salary.getStatus()
        ));
    }
}
