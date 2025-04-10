package com.hrms.backend.controller;

import com.hrms.backend.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    // Mark a single notification as read
    @GetMapping("/mark-as-read/{userId}/{notificationId}")
    public ResponseEntity<String> markNotificationAsRead(
            @PathVariable Long userId, @PathVariable Long notificationId) {
        boolean success = notificationService.markNotificationAsRead(userId, notificationId);
        if (success) {
            return ResponseEntity.ok("Notification marked as read.");
        } else {
            return ResponseEntity.status(404).body("Notification not found.");
        }
    }

    // Mark all notifications as read for a user
    @GetMapping("/mark-all-as-read/{userId}/{role}")
    public ResponseEntity<String> markAllNotificationsAsRead(@PathVariable Long userId, @PathVariable String role) {
        boolean success = notificationService.markAllNotificationsAsRead(userId,role);
        if (success) {
            return ResponseEntity.ok("All notifications marked as read.");
        } else {
            return ResponseEntity.status(404).body("No notifications found for the user.");
        }
    }
}
