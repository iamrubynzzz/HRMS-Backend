package com.hrms.backend.repository;

import com.hrms.backend.dto.ConsolidatedSalaryDTO;
import com.hrms.backend.entities.ConsolidatedSalary;
import com.hrms.backend.entities.Salary;
import com.hrms.backend.entities.SalaryStatus;
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
public interface ConsolidatedSalaryRepository extends JpaRepository<ConsolidatedSalary, Long> {

    @Query("SELECT new com.hrms.backend.dto.ConsolidatedSalaryDTO( " +
            "c.id, c.calculationDate, c.totalNumberOfEmployees, c.totalExpense, c.status) " +
            "FROM ConsolidatedSalary c " +
            "WHERE (:startDate IS NULL OR c.calculationDate >= :startDate) " +
            "AND (:endDate IS NULL OR c.calculationDate <= :endDate) " +
            "ORDER BY c.calculationDate DESC")
    Page<ConsolidatedSalaryDTO> findFilteredConsolidatedSalaries(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    Optional<ConsolidatedSalary> findById(Long id);
}
