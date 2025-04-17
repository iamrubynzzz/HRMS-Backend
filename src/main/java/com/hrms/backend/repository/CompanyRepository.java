package com.hrms.backend.repository;

import com.hrms.backend.dto.AttendanceDTO;
import com.hrms.backend.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String companyName);

    Page<Company> findByCompanyStatus(CompanyStatus status, Pageable pageable);

    Page<Company> findByNameContainingIgnoreCaseAndCompanyStatus(String name, CompanyStatus status, Pageable pageable);


    boolean existsByEmail(String email);

    boolean existsByRegistrationNumber(String registrationNumber);
}
