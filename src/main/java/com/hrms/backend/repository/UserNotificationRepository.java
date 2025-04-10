package com.hrms.backend.repository;

import com.hrms.backend.entities.Notification;
import com.hrms.backend.entities.User;
import com.hrms.backend.entities.UserInfo;
import com.hrms.backend.entities.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Integer> {
    UserNotification findUserNotificationByUserAndNotification(User user, Notification notification);
}
