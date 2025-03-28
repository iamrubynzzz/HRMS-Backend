package com.hrms.backend.controller;

import com.hrms.backend.dto.ConsolidatedSalaryDTO;
import com.hrms.backend.services.ConsolidatedSalaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/consolidated-salaries")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class ConsolidatedSalaryController {

    private final ConsolidatedSalaryService consolidatedSalaryService;

    @GetMapping
    public ResponseEntity<Page<ConsolidatedSalaryDTO>> getFilteredConsolidatedSalaries(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ConsolidatedSalaryDTO> consolidatedSalaries =
                consolidatedSalaryService.getFilteredConsolidatedSalaries(startDate, endDate, page, size);

        return ResponseEntity.ok(consolidatedSalaries);
    }
}

