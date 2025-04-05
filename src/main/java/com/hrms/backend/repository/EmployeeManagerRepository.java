package com.hrms.backend.repository;

import com.hrms.backend.entities.EmployeeManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeManagerRepository extends JpaRepository<EmployeeManager, Integer> {
    Optional<EmployeeManager> findByEmployeeId(Integer employeeId);
    boolean existsByEmployeeId(Integer employeeId);

    @Query("SELECT em.employeeId FROM EmployeeManager em WHERE em.managerId = :managerId")
    List<Integer> findEmployeeIdsByManagerId(@Param("managerId") Integer managerId);


}

