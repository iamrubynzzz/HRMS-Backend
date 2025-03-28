package com.hrms.backend.repository;

import com.hrms.backend.entities.Notification;
import com.hrms.backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserAndIsReadFalse(User user);  // Get unread notifications

  //  List<Notification> findByUserUsernameAndIsReadFalse(String username);

    void deleteByUserId(Integer id);
}
