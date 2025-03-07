package com.hrms.backend.services;

import jakarta.mail.MessagingException;

public interface EmailService {
    boolean sendEmail(String to, String subject, String content) throws MessagingException;
    void processEmail();
}
