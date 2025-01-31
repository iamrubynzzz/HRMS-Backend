package com.hrms.backend.repository;

import com.hrms.backend.entities.User;
import com.hrms.backend.entities.UserInfo;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Integer> {
    @Modifying
    @Transactional
    @Query("DELETE FROM UserInfo u WHERE u.user.id = :userId")
    void deleteByUserId(Integer userId);

    // Custom query to find UserInfo by user ID
    Optional<UserInfo> findByUserId(Integer userId);

    Optional<Object> findByUser(User user);
}

