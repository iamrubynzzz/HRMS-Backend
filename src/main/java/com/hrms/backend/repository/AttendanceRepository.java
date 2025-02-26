package com.hrms.backend.repository;

import com.hrms.backend.dto.AttendanceDTO;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.AttendanceStatus;
import com.hrms.backend.entities.User;
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
}
