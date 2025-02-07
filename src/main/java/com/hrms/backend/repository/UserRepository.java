package com.hrms.backend.repository;

import com.hrms.backend.entities.Role;
import com.hrms.backend.entities.User;
import com.hrms.backend.entities.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository <User,Integer>{
    Optional<User> findByEmail(String email);
    User findByRole(Role role);
    boolean existsByRole(Role role);

    boolean existsByEmail(String email);

    List<User> findByStatus(Status userStatus);

    Optional<User> findById(int id);

    Optional<User> findByRfid(String rfid);
    boolean existsByRfid(String rfid);
    Optional<User> findFirstByRole(Role role);
}
