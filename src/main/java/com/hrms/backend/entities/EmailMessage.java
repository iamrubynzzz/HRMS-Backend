package com.hrms.backend.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "email_message")
public class EmailMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "message", length = 500)
    private String message;
    private String subject;
    private String recipientAddress;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmailStatus status;
    @Column(name = "company_name", nullable = false)
    private String companyName;
}
