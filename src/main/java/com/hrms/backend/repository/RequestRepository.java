package com.hrms.backend.repository;

import com.hrms.backend.entities.Request;
import com.hrms.backend.entities.RequestType;
import com.hrms.backend.entities.Status;
import com.hrms.backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByUser(User user);
    List<Request> findByStatus(Status status);

    List<Request> findByCreatedDate(LocalDate date);

    List<Request> findByUserAndRequestType(User user, RequestType requestType);

    List<Request> findByUserAndRequestTypeAndStartDate(User user, RequestType requestType, LocalDate startDate);

    List<Request> findByUserAndRequestTypeAndStatus(User user, RequestType requestType, Status status);

    // Refactor to a single date range check
    List<Request> findByUserAndRequestTypeAndStatusAndStartDateBetween(
            User user, RequestType requestType, Status status, LocalDate startDate, LocalDate endDate);

}
