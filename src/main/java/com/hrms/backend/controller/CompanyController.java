package com.hrms.backend.controller;

import com.hrms.backend.dto.CompanyRequestDTO;
import com.hrms.backend.entities.Company;
import com.hrms.backend.services.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @GetMapping("/list")
    public ResponseEntity<List<Company>> getAllCompanies() {
        List<Company> companies = companyService.getAllCompanies();
        return ResponseEntity.ok(companies);
    }

    // API to get company in super admin view in the frontend
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<Company>> getCompanies(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Company> companies = companyService.getCompanies(name, page, size);
        return ResponseEntity.ok(companies);
    }

    // API to create company
    @PostMapping("/create")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Company> createCompany(@RequestBody CompanyRequestDTO request) {
        Company createdCompany = companyService.createCompany(request);
        return ResponseEntity.ok(createdCompany);
    }

    // API to update company
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> updateCompany(@PathVariable Integer id, @RequestBody CompanyRequestDTO companyRequestDTO) {
        companyService.updateCompany(id, companyRequestDTO);
        return ResponseEntity.ok("Company information updated successfully");
    }

    @DeleteMapping("delete/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> deleteCompany(@PathVariable Integer id) {
        companyService.deleteCompany(id);
        return ResponseEntity.ok("Company deactivated successfully");
    }

}

