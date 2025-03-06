package com.hrms.backend.repository;

import com.hrms.backend.dto.AttendanceDTO;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.AttendanceStatus;
import com.hrms.backend.entities.Company;
import com.hrms.backend.entities.User;
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
}
