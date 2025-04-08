package com.hrms.backend.repository;

import com.hrms.backend.dto.AttendanceDTO;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.AttendanceStatus;
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
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByUserAndDate(User user, LocalDate date);
    Optional<Attendance> findByUserIdAndDate(int userId,LocalDate date);
    List<Attendance> findByUserIdAndDateBetween(int userId, LocalDate startDate, LocalDate endDate);

    void deleteByUser(User user);

    boolean existsByUser(User user);
   /* List<Attendance> findByStatusAndDateBetween(AttendanceStatus status, LocalDate startDate, LocalDate endDate);*/

    @Query("SELECT new com.hrms.backend.dto.AttendanceDTO(a.id, a.user.name, a.user.id, a.date, a.punchIn, a.punchOut, a.status) " +
            "FROM Attendance a WHERE a.status = :status AND a.date BETWEEN :startDate AND :endDate")
    List<AttendanceDTO> findByStatusAndDateBetween(
            @Param("status") AttendanceStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    //All attendance list of all user for admin view
    @Query("SELECT a FROM Attendance a " +
            "JOIN a.user u " +
            "WHERE (:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:startDate IS NULL OR a.date >= :startDate) " +
            "AND (:endDate IS NULL OR a.date <= :endDate) " +
            "AND (:status IS NULL OR a.status = :status)")
    Page<Attendance> findAllFiltered(
            @Param("name") String name,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") AttendanceStatus status,
            Pageable pageable);

    //All attendance list for employee view where employee can see their own attendance only
    @Query("SELECT a FROM Attendance a " +
            "JOIN a.user e " +  // Assuming attendance is related to an employee
            "WHERE e.id = :employeeId " +
            "AND (:startDate IS NULL OR a.date >= :startDate) " +
            "AND (:endDate IS NULL OR a.date <= :endDate) " +
            "AND (:attendanceStatus IS NULL OR a.status = :attendanceStatus) " +
            "AND (:name IS NULL OR e.name LIKE %:name%)")  // Filtering by employee name
    Page<Attendance> findByEmployeeIdAndFilters(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("attendanceStatus") AttendanceStatus attendanceStatus,
            @Param("name") String name,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.status = :status AND a.date = :date")
    long countByStatusAndDate(@Param("status") AttendanceStatus status, @Param("date") LocalDate date);

    @Query("SELECT a FROM Attendance a WHERE a.user.id IN :employeeIds " +
            "AND (:startDate IS NULL OR a.date >= :startDate) " +
            "AND (:endDate IS NULL OR a.date <= :endDate) " +
            "AND (:status IS NULL OR a.status = :status)")
    Page<Attendance> findByEmployeeIdsAndFilters(@Param("employeeIds") List<Integer> employeeIds,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("status") AttendanceStatus status,
                                                 Pageable pageable);

    @Query("SELECT a FROM Attendance a WHERE a.user.id IN :employeeIds " +
            "AND (:startDate IS NULL OR a.date >= :startDate) " +
            "AND (:endDate IS NULL OR a.date <= :endDate) " +
            "AND (:status IS NULL OR a.status = :status) " +
            "AND (:name IS NULL OR a.user.name LIKE %:name%)")
    Page<Attendance> findByEmployeeIdsAndFiltersAndName(
            @Param("employeeIds") List<Integer> employeeIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") AttendanceStatus status,
            @Param("name") String name,
            Pageable pageable);


    // Custom query to get attendance by employeeId and date range
    @Query("SELECT a FROM Attendance a WHERE a.user.name = :name AND a.date BETWEEN :startDate AND :endDate")
    List<Attendance> findAttendanceByEmployeeAndDateRange(String name, LocalDate startDate, LocalDate endDate);
}

