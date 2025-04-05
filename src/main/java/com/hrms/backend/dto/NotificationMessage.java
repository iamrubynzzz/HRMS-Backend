package com.hrms.backend.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationMessage {
    private String message;
    private String recipientRole; // Admin, SuperAdmin, etc.
    private Long companyId; // To ensure the right admin receives the notification
}