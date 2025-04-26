package com.hrms.backend.services;

import com.hrms.backend.entities.EmailMessage;
import jakarta.mail.MessagingException;
import org.springframework.data.domain.Page;

import java.util.concurrent.CompletableFuture;

public interface EmailService {
 //   boolean sendEmail(String to, String subject, String content) throws MessagingException;

    CompletableFuture<Boolean> sendEmail(String to, String subject, String content) throws MessagingException;
    void processEmail();

    Page<EmailMessage> getPaginatedEmailMessages(int page, int size, String recipientAddress);

}
