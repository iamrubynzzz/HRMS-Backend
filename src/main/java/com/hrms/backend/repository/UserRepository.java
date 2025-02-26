package com.hrms.backend.repository;

import com.hrms.backend.entities.Role;
import com.hrms.backend.entities.User;
import com.hrms.backend.entities.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    // Existing methods
    Optional<User> findByEmail(String email);
    User findByRole(Role role);
    boolean existsByRole(Role role);
    boolean existsByEmail(String email);
    List<User> findByStatus(Status userStatus);
    Optional<User> findById(int id);
    Optional<User> findByRfid(String rfid);
    boolean existsByRfid(String rfid);
    Optional<User> findFirstByRole(Role role);


    // New method for filtering and pagination
    @Query("SELECT u FROM User u WHERE " +
            "(:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(u.role = :roleEmployee OR u.role = :roleManager) AND " +
            "u.status = :status")
    Page<User> findAllFiltered(
            @Param("name") String name,
            @Param("roleEmployee") Role roleEmployee,
            @Param("roleManager") Role roleManager,
            @Param("status") Status status,
            Pageable pageable
    );
}