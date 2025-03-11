package com.hrms.backend.services.impl;

import com.hrms.backend.entities.Company;
import com.hrms.backend.repository.CompanyRepository;
import com.hrms.backend.services.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
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
}
