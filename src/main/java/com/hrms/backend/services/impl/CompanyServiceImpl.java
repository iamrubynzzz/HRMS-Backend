package com.hrms.backend.services.impl;

import com.hrms.backend.dto.CompanyRequestDTO;
import com.hrms.backend.entities.Company;
import com.hrms.backend.entities.CompanyStatus;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.exception.ResourceNotFoundException;
import com.hrms.backend.repository.CompanyRepository;
import com.hrms.backend.services.CompanyService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyServiceImpl implements CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Override
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @Override
    public Page<Company> getCompanies(String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        if (name != null && !name.isEmpty()) {
            return companyRepository.findByNameContainingIgnoreCaseAndCompanyStatus(
                    name,
                    CompanyStatus.ACTIVE,
                    pageable
            );
        }

        return companyRepository.findByCompanyStatus(CompanyStatus.ACTIVE, pageable);
    }


    @Override
    public Company createCompany(CompanyRequestDTO request) {
        Company company = new Company();
        company.setName(request.getName());
        company.setAddress(request.getAddress());
        company.setPhone(request.getPhone());
        company.setEmail(request.getEmail());
        company.setWebsite(request.getWebsite());
        company.setIndustryType(request.getIndustryType());
        company.setRegistrationNumber(request.getRegistrationNumber());
        company.setEstablishedDate(request.getEstablishedDate());

        return companyRepository.save(company);
    }

    @Override
    public void updateCompany(Integer id, CompanyRequestDTO companyRequestDTO) {
        Company company = companyRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new ResourceNotFoundException("Company with ID " + id + " not found"));

        if (companyRequestDTO.getName() != null && !companyRequestDTO.getName().isEmpty()) {
            company.setName(companyRequestDTO.getName());
        }

        if (companyRequestDTO.getAddress() != null && !companyRequestDTO.getAddress().isEmpty()) {
            company.setAddress(companyRequestDTO.getAddress());
        }

        if (companyRequestDTO.getPhone() != null && !companyRequestDTO.getPhone().isEmpty()) {
            company.setPhone(companyRequestDTO.getPhone());
        }

        // Update email only if it is changed and unique
        if (companyRequestDTO.getEmail() != null
                && !companyRequestDTO.getEmail().isEmpty()
                && !companyRequestDTO.getEmail().equals(company.getEmail())) {

            if (companyRepository.existsByEmail(companyRequestDTO.getEmail())) {
                throw new GenericException("Email " + companyRequestDTO.getEmail() + " is already in use.", HttpStatus.CONFLICT);
            }

            company.setEmail(companyRequestDTO.getEmail());
        }

        if (companyRequestDTO.getWebsite() != null && !companyRequestDTO.getWebsite().isEmpty()) {
            company.setWebsite(companyRequestDTO.getWebsite());
        }

        if (companyRequestDTO.getIndustryType() != null && !companyRequestDTO.getIndustryType().isEmpty()) {
            company.setIndustryType(companyRequestDTO.getIndustryType());
        }

        if (companyRequestDTO.getRegistrationNumber() != null
                && !companyRequestDTO.getRegistrationNumber().isEmpty()
                && !companyRequestDTO.getRegistrationNumber().equals(company.getRegistrationNumber())) {

            if (companyRepository.existsByRegistrationNumber(companyRequestDTO.getRegistrationNumber())) {
                throw new GenericException("Registration number " + companyRequestDTO.getRegistrationNumber() + " is already in use.", HttpStatus.CONFLICT);
            }

            company.setRegistrationNumber(companyRequestDTO.getRegistrationNumber());
        }


        if (companyRequestDTO.getEstablishedDate() != null) {
            company.setEstablishedDate(companyRequestDTO.getEstablishedDate());
        }


        companyRepository.save(company);
    }

    @Transactional
    @Override
    public void deleteCompany(Integer id) {
        Company company = companyRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new ResourceNotFoundException("Company with ID " + id + " not found"));

        try {
            company.setCompanyStatus(CompanyStatus.INACTIVE);
            companyRepository.save(company);
            System.out.println("Company " + id + " marked as INACTIVE.");
        } catch (Exception ex) {
            throw new GenericException("Failed to deactivate company with ID: " + id + ". Reason: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
