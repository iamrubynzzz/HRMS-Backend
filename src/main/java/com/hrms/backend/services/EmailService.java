package com.hrms.backend.services;

import jakarta.mail.MessagingException;

import java.util.concurrent.CompletableFuture;

public interface EmailService {
 //   boolean sendEmail(String to, String subject, String content) throws MessagingException;

    CompletableFuture<Boolean> sendEmail(String to, String subject, String content) throws MessagingException;
    void processEmail();
}
