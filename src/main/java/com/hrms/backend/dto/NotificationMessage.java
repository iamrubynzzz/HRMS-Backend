package com.hrms.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String to;        // Username of the recipient (for private messages)
    private String message;   // Notification content
    private String type;      // Type of notification (REQUEST, LEAVE, etc.)
}
