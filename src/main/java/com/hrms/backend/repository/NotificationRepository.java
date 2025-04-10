package com.hrms.backend.repository;

import com.hrms.backend.entities.Notification;
import com.hrms.backend.entities.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    void deleteByUserId(Integer id);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.status = 'READ' WHERE n.id = :notificationId AND n.user.id = :userId")
    int markNotificationAsRead(Long userId, Long notificationId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.status = 'READ' WHERE n.user.id = :userId")
    int markAllNotificationsAsRead(Long userId);

    @Query("select n from Notification n join UserNotification u on n.id = u.notification.id where n.user.id = :userId and u.id is null")
    List<Notification> findUnreadNotifications(Long userId);

}
