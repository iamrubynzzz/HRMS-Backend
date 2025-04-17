package com.hrms.backend.services;
import com.hrms.backend.dto.CompanyRequestDTO;
import com.hrms.backend.entities.Company;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CompanyService {
    List<Company> getAllCompanies();

    Page<Company> getCompanies(String name, int page, int size);

    Company createCompany(CompanyRequestDTO request);

    void updateCompany(Integer id, CompanyRequestDTO companyRequestDTO);

    void deleteCompany(Integer id);
}
