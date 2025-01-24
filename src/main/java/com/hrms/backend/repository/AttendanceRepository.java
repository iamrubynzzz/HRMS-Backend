package com.hrms.backend.repository;

import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

    List<Attendance> findByUserId(int userId);

    @Query("SELECT a FROM Attendance a WHERE a.user.id = :userId AND a.date = :date ORDER BY a.punchIn DESC")
    Optional<Attendance> findTopByUserIdAndDateOrderByPunchInDesc(@Param("userId") int userId, @Param("date") LocalDate date);

    Optional<Attendance> findByUserAndDateAndPunchOutIsNull(User user, LocalDate date);

    Optional<Attendance> findByUserAndDate(User user, LocalDate today);
}
