package com.hrms.backend.services;

import com.hrms.backend.entities.Notification;
import com.hrms.backend.entities.NotificationType;
import com.hrms.backend.entities.Request;
import com.hrms.backend.entities.User;

import java.util.List;

public interface NotificationService {

    boolean markNotificationAsRead(Long userId, Long notificationId);

    boolean markAllNotificationsAsRead(Long userId, String role);
}
