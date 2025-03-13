package com.hrms.backend.controller;

import com.hrms.backend.dto.SalaryDTO;
import com.hrms.backend.entities.Salary;
import com.hrms.backend.entities.SalaryStatus;
import com.hrms.backend.repository.SalaryRepository;
import com.hrms.backend.services.SalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/salaries")
public class SalaryController {

    @Autowired
    private SalaryRepository salaryRepository;

    @Autowired
    private SalaryService salaryService;

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

    // GET API to retrieve salaries for all user
    @GetMapping("/all")
    public ResponseEntity<Page<SalaryDTO>> getAllSalaries(
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

        Page<Salary> salaries = salaryService.getSalaries(employeeName, startDate, endDate, pageable);

        Page<SalaryDTO> salaryDTOs = salaries.map(salary -> new SalaryDTO(
                salary.getId(),
                salary.getUser().getName(),
                salary.getStatus(),
                salary.getGrossSalary(),
                salary.getTaxDeduction(),
                salary.getOvertimePayTotal(),
                salary.getAllowanceAmountTotal(),
                salary.getNetSalary(),
                salary.getCalculationDate()
        ));

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

    //API to release salary
    @PutMapping("/release/{salaryId}")
    public ResponseEntity<String> releaseSalary(@PathVariable Long salaryId) {
        Optional<Salary> salary = salaryRepository.findById(salaryId);
        if (salary.isPresent()) {
            Salary s = salary.get();
            if (s.getStatus() == SalaryStatus.APPROVED) {
                s.setStatus(SalaryStatus.RELEASED);
                salaryRepository.save(s);
                return ResponseEntity.ok("Salary released successfully.");
            }
            return ResponseEntity.badRequest().body("Salary must be approved before release.");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Salary not found.");
    }



    // GET API to retrieve salaries for a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Salary>> getSalariesByUser(@PathVariable int userId) {
        List<Salary> salaries = salaryRepository.findByUserId(userId);
        return ResponseEntity.ok(salaries);
    }
}
