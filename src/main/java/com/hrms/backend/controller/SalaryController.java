package com.hrms.backend.controller;

import com.hrms.backend.dto.SalaryDTO;
import com.hrms.backend.entities.*;
import com.hrms.backend.exception.ResourceNotFoundException;
import com.hrms.backend.repository.EmailMessageRepository;
import com.hrms.backend.repository.EmployeeManagerRepository;
import com.hrms.backend.repository.SalaryRepository;
import com.hrms.backend.services.EmailService;
import com.hrms.backend.services.SalaryReportService;
import com.hrms.backend.services.SalaryService;
import com.hrms.backend.services.UserService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/salaries")
@CrossOrigin(origins = "http://localhost:3000")
public class SalaryController {

    @Autowired
    private SalaryRepository salaryRepository;

    @Autowired
    private SalaryService salaryService;
    private final EmailService emailService;
    private  final UserService userService;
    private final EmailMessageRepository emailMessageRepository;
    private final EmployeeManagerRepository employeeManagerRepository;

    @Autowired
    private SalaryReportService salaryReportService;
    public SalaryController(EmailService emailService, UserService userService, EmailMessageRepository emailMessageRepository, EmployeeManagerRepository employeeManagerRepository) {
        this.emailService = emailService;
        this.userService = userService;
        this.emailMessageRepository = emailMessageRepository;
        this.employeeManagerRepository = employeeManagerRepository;
    }

    @PostMapping("/calculate")
    public ResponseEntity<String> calculateSalaries() {
        salaryService.calculateSalaryForAllEmployees();
        return ResponseEntity.ok("Salary calculation completed successfully.");
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSalaryOverview() {
        double totalSalaries = salaryRepository.sumNetSalaryByStatus(SalaryStatus.RELEASED).orElse(0.0);
        double pendingPayments = salaryRepository.sumNetSalaryByStatus(SalaryStatus.PENDING_REVIEW).orElse(0.0);
        double totalDeductions = salaryRepository.sumTaxDeductions().orElse(0.0);

        Map<String, Object> overview = new HashMap<>();
        overview.put("totalSalaries", totalSalaries);
        overview.put("pendingPayments", pendingPayments);
        overview.put("totalDeductions", totalDeductions);

        return ResponseEntity.ok(overview);
    }

    // GET API to retrieve salaries for all user based on consolidated salary id
    @GetMapping("/by-consolidated-salary/{id}")
    public ResponseEntity<Page<SalaryDTO>> getSalariesByConsolidatedSalaryId(
            @PathVariable Long id,
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "calculationDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = PageRequest.of(page, size,
                sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending()
                        : Sort.by(sortBy).ascending());

        Page<SalaryDTO> salaryDTOs = salaryService.getSalariesByConsolidatedSalaryId(id, employeeName, startDate, endDate, pageable);
        return ResponseEntity.ok(salaryDTOs);
    }

    //API to approve salary
    @PutMapping("/approve/{salaryId}")
    public ResponseEntity<String> approveSalary(@PathVariable Long salaryId) {
        Optional<Salary> salary = salaryRepository.findById(salaryId);
        if (salary.isPresent()) {
            Salary s = salary.get();
            s.setStatus(SalaryStatus.APPROVED);
            salaryRepository.save(s);
            return ResponseEntity.ok("Salary approved successfully.");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Salary not found.");
    }

    @PutMapping("/approve/by-consolidated-salary/{consolidatedSalaryId}")
    public ResponseEntity<String> approveSalariesByConsolidatedSalary(@PathVariable Long consolidatedSalaryId) {
        int updatedCount = salaryService.approveSalariesByConsolidatedSalary(consolidatedSalaryId);

        if (updatedCount > 0) {
            return ResponseEntity.ok("Successfully approved " + updatedCount + " salaries.");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No salaries found for the given Consolidated Salary ID.");
    }


    @PutMapping("/release/by-consolidated-salary/{consolidatedSalaryId}")
    public ResponseEntity<String> releaseSalariesByConsolidatedSalary(@PathVariable Long consolidatedSalaryId) throws MessagingException {
        int releasedCount = salaryService.releaseSalariesByConsolidatedSalary(consolidatedSalaryId);
        System.out.println("==============================");
        if (releasedCount > 0) {
            return ResponseEntity.ok("Successfully released " + releasedCount + " salaries and sent email notifications.");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No salaries found for the given Consolidated Salary ID.");
    }


    // GET API to retrieve salaries for a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Salary>> getSalariesByUser(@PathVariable int userId) {
        List<Salary> salaries = salaryRepository.findByUserId(userId);
        return ResponseEntity.ok(salaries);
    }

    //API to generate salary report
    @GetMapping("generate/{salaryId}/report")
    public ResponseEntity<String> downloadSalaryReport(@PathVariable Long salaryId) {
        String filePath = salaryReportService.generateSalaryReportById(salaryId);

        // Return the file path in the response
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body("Salary report saved to: " + filePath);
    }

    // For the employee view of payroll
    // Get monthly payroll for the logged-in employee with pagination and filtering options
    @GetMapping("/my-salary")
    public ResponseEntity<Page<SalaryDTO>> getMySalary(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "calculationDate,desc") String sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        String username = principal.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Parse the sort parameter
        String[] sortParams = sort.split(",");
        Sort.Direction direction = Sort.Direction.fromString(sortParams[1]);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        // Fetch the monthly salary with pagination and date filters
        Page<SalaryDTO> mySalary = salaryService.getMonthlyPayroll(user, startDate, endDate, pageable);
        return ResponseEntity.ok(mySalary);
    }

    // For the manager view of payroll to see their own as well as assigned employees salary
    @GetMapping("/manager-payroll")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Page<SalaryDTO>> getManagerAndEmployeesPayroll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {

        System.out.println("================ Controller reached");

        Optional<User> managerOptional = userService.findByUsername(principal.getName());
        if (managerOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        User manager = managerOptional.get();

        // Handle default date range
        if (startDate == null) {
            startDate = LocalDate.of(2000, 1, 1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Manager's payroll
        Page<SalaryDTO> managerPayroll = salaryService.getMonthlyPayrollForUser(manager, startDate, endDate, page, size);

        // Employee IDs under manager
        List<Integer> managedEmployeeIds = employeeManagerRepository.findEmployeeIdsByManagerId(manager.getId());
        List<Long> managedEmployeeLongIds = managedEmployeeIds.stream().map(Integer::longValue).collect(Collectors.toList());

        // Employees' payroll
        Page<SalaryDTO> employeesPayroll = salaryService.getMonthlyPayrollForMultipleUsers(
                managedEmployeeLongIds, startDate, endDate, page, size);

        // Combine
        List<SalaryDTO> combinedContent = new ArrayList<>();
        combinedContent.addAll(managerPayroll.getContent());
        combinedContent.addAll(employeesPayroll.getContent());

        Page<SalaryDTO> combinedPage = new PageImpl<>(combinedContent,
                PageRequest.of(page, size),
                managerPayroll.getTotalElements() + employeesPayroll.getTotalElements());

        return ResponseEntity.ok(combinedPage);
    }

}
