package com.hrms.backend.repository;

import com.hrms.backend.dto.LeaveBalanceDTO;
import com.hrms.backend.entities.Request;
import com.hrms.backend.entities.RequestType;
import com.hrms.backend.entities.Status;
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

    boolean existsByUserIdAndRequestTypeInAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId,
            List<RequestType> requestTypes,
            List<Status> statuses,
            LocalDate endDate,
            LocalDate startDate
    );

    void deleteByUser(User user);
    Page<Request> findByUser(User user, Pageable pageable);

    //Used for employee view of requests
    @Query("SELECT r FROM Request r WHERE r.user = :user AND (:status IS NULL OR r.status = :status) AND (:date IS NULL OR r.createdDate = :date) ORDER BY r.createdDate DESC")
    Page<Request> findByUserAndFilters(
            @Param("user") User user,
            @Param("status") Status status,
            @Param("date") LocalDate date,
            Pageable pageable
    );


    @Query("SELECT r FROM Request r JOIN r.user u WHERE "
            + "(:status IS NULL OR r.status = :status) "
            + "AND (:date IS NULL OR r.createdDate = :date) "
            + "AND (:employeeName IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :employeeName, '%'))) "
            + "ORDER BY r.createdDate DESC")
    Page<Request> findAllRequests(
            @Param("status") Status status,
            @Param("date") LocalDate date,
            @Param("employeeName") String employeeName,
            Pageable pageable
    );

}
