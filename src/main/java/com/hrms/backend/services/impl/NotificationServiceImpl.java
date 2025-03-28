package com.hrms.backend.services.impl;

import com.hrms.backend.entities.Notification;
import com.hrms.backend.entities.NotificationType;
import com.hrms.backend.entities.Request;
import com.hrms.backend.entities.User;
import com.hrms.backend.repository.NotificationRepository;
import com.hrms.backend.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;


@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendNotification(User user, String message, NotificationType type) {
        Notification notification = new Notification(user, message, type);
        notificationRepository.save(notification);

        // WebSocket notification (for real-time updates)
        messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/notifications", message);

    }
}
