package com.hrms.backend.repository;

import com.hrms.backend.entities.EmailMessage;
import com.hrms.backend.entities.EmailStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, Integer> {
    List<EmailMessage> findByStatus (EmailStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE EmailMessage e SET e.status = :status WHERE e.id = :id")
    void updateEmailStatusById(int id, EmailStatus status);
}

