package com.hrms.backend.repository;

import com.hrms.backend.entities.EmployeeManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeManagerRepository extends JpaRepository<EmployeeManager, Integer> {
    Optional<EmployeeManager> findByEmployeeId(Integer employeeId);
    boolean existsByEmployeeId(Integer employeeId);
}

