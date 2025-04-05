package com.hrms.backend.controller;

import com.hrms.backend.dto.SalaryDTO;
import com.hrms.backend.entities.*;
import com.hrms.backend.exception.ResourceNotFoundException;
import com.hrms.backend.repository.EmailMessageRepository;
import com.hrms.backend.repository.SalaryRepository;
import com.hrms.backend.services.EmailService;
import com.hrms.backend.services.SalaryReportService;
import com.hrms.backend.services.SalaryService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/salaries")
@CrossOrigin(origins = "http://localhost:3000")
public class SalaryController {

    @Autowired
    private SalaryRepository salaryRepository;

    @Autowired
    private SalaryService salaryService;
    private final EmailService emailService;
    private final EmailMessageRepository emailMessageRepository;

    @Autowired
    private SalaryReportService salaryReportService;
    public SalaryController(EmailService emailService, EmailMessageRepository emailMessageRepository) {
        this.emailService = emailService;
        this.emailMessageRepository = emailMessageRepository;
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
    @GetMapping("/{salaryId}/report")
    public ResponseEntity<String> downloadSalaryReport(@PathVariable Long salaryId) {
        String filePath = salaryReportService.generateSalaryReportById(salaryId);

        // Return the file path in the response
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body("Salary report saved to: " + filePath);
    }
}
