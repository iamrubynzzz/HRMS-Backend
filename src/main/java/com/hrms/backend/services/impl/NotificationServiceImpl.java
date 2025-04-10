package com.hrms.backend.services.impl;

import com.hrms.backend.entities.Notification;
import com.hrms.backend.entities.UserNotification;
import com.hrms.backend.repository.NotificationRepository;
import com.hrms.backend.repository.UserNotificationRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserNotificationRepository userNotificationRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean markNotificationAsRead(Long userId, Long notificationId) {
        // Update the status of a specific notification to READ
        Notification notification = notificationRepository.findById(notificationId).get();
        if (userNotificationRepository.findUserNotificationByUserAndNotification(userRepository.findById(Math.toIntExact(userId)).get(), notificationRepository.findById(notificationId).get()) == null) {
            saveUserNotification(userId, notification.getId());
        }
        return true;  // Return true if at least one notification was updated
    }

    @Override
    public boolean markAllNotificationsAsRead(Long userId, String role) {
        PageRequest pageRequest = PageRequest.of(0, 100, Sort.by(Sort.Order.desc("createdAt")));
        Page<Notification> page = notificationRepository.findAll(pageRequest);
        List<Notification> notificationList = page.getContent();
        List<Notification> filteredNotificationList = new ArrayList<>();
        filteredNotificationList.addAll(notificationList.stream().filter(a -> a.getNotificationGroup().contains(role.toUpperCase())).toList());
        filteredNotificationList.addAll(notificationList.stream().filter(a -> a.getUser().getId() == Integer.parseInt(String.valueOf(userId)) && !filteredNotificationList.contains(a)).toList());
        AtomicInteger count = new AtomicInteger();
        filteredNotificationList.forEach(notification -> {
            if (userNotificationRepository.findUserNotificationByUserAndNotification(userRepository.findById(Math.toIntExact(userId)).get(), notificationRepository.findById(notification.getId()).get()) == null) {

                saveUserNotification(userId, notification.getId());
            }
            count.getAndIncrement();
        });
        return count.get() > 0;
    }

    private void saveUserNotification(Long userId, Long notificationId) {
        UserNotification userNotification = new UserNotification();
        userNotification.setUser(userRepository.findById(Integer.parseInt(userId.toString())).get());
        userNotification.setNotification(notificationRepository.findById(notificationId).get());
        userNotification.setRead(true);
        userNotificationRepository.save(userNotification);
    }
}
