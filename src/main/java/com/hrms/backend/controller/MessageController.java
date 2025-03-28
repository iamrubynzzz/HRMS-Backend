package com.hrms.backend.controller;

import com.hrms.backend.dto.NotificationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.hrms.backend.entities.Request;

@Controller
public class MessageController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    // Public notifications (e.g., to all Admins)
    @MessageMapping("/application")
    @SendTo("all/messages")
    public void sendPublicNotification(@Payload NotificationMessage message) {
        System.out.println("------->>>>"+message);
        simpMessagingTemplate.convertAndSend("/topic/notifications", message);
    }

    // Private notifications (e.g., to a specific Admin)
    @MessageMapping("/private")
    public void sendToSpecificUser(@Payload NotificationMessage message) {
        simpMessagingTemplate.convertAndSendToUser(message.getTo(), "/queue/notifications", message);
    }
}
