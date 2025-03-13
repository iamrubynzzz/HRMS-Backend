package com.hrms.backend.repository;

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
public interface SalaryRepository extends JpaRepository<Salary, Long> {
    List<Salary> findByUserId(int userId);
    Optional<Salary> findByUserIdAndCalculationDate(int userId, LocalDate date);

    void deleteByUser(User user);

    boolean existsByUser(User user);

    @Query("SELECT SUM(s.netSalary) FROM Salary s WHERE s.status = :status")
    Optional<Double> sumNetSalaryByStatus(@Param("status") SalaryStatus status);


    @Query("SELECT SUM(s.taxDeduction) FROM Salary s")
    Optional<Double> sumTaxDeductions();


    @Query("SELECT s FROM Salary s WHERE " +
            "(:employeeName IS NULL OR s.user.name LIKE %:employeeName%) " +
            "AND (:startDate IS NULL OR s.calculationDate >= :startDate) " +
            "AND (:endDate IS NULL OR s.calculationDate <= :endDate)")
    Page<Salary> findSalaries(@Param("employeeName") String employeeName,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              Pageable pageable);


}
