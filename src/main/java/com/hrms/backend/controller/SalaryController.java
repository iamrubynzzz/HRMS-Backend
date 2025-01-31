package com.hrms.backend.controller;

import com.hrms.backend.entities.Salary;
import com.hrms.backend.repository.SalaryRepository;
import com.hrms.backend.services.SalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // GET API to retrieve salaries for a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Salary>> getSalariesByUser(@PathVariable int userId) {
        List<Salary> salaries = salaryRepository.findByUserId(userId);
        return ResponseEntity.ok(salaries);
    }
}
