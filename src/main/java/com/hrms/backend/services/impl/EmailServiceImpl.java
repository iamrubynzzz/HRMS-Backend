package com.hrms.backend.services.impl;


import com.hrms.backend.entities.EmailMessage;
import com.hrms.backend.entities.EmailStatus;
import com.hrms.backend.repository.EmailMessageRepository;
import com.hrms.backend.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service

public class EmailServiceImpl implements EmailService {
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private EmailMessageRepository emailMessageRepository;

    @Async
    public CompletableFuture<Boolean> sendEmail(String to, String subject, String content) throws MessagingException {
        String sanitizedEmail = to.trim();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(sanitizedEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);

        try {
            mailSender.send(message);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }


    @Override
    public void processEmail() {
        List<EmailMessage> emailMessages = emailMessageRepository.findByStatus(EmailStatus.PENDING);
        System.out.println("::: SENDING "+emailMessages.size()+" NO. of Emails ::::");
        emailMessages.forEach(emailMessage -> {
            try {
                CompletableFuture<Boolean> future = sendEmail(emailMessage.getRecipientAddress(), emailMessage.getSubject(), emailMessage.getMessage());
                future.thenAccept(sent -> {
                    if (sent) {
                        emailMessageRepository.updateEmailStatusById(emailMessage.getId(), EmailStatus.SUCCESS);
                    } else {
                        emailMessageRepository.updateEmailStatusById(emailMessage.getId(), EmailStatus.FAILED);
                    }
                });

            } catch (Exception e) {
                emailMessageRepository.updateEmailStatusById(emailMessage.getId(), EmailStatus.FAILED);
                e.printStackTrace();
            }
        });
    }


    @Override
    public Page<EmailMessage> getPaginatedEmailMessages(int page, int size, String recipientAddress) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        if (recipientAddress != null && !recipientAddress.isEmpty()) {
            return emailMessageRepository.findByRecipientAddressContainingIgnoreCase(recipientAddress, pageable);
        } else {
            return emailMessageRepository.findAll(pageable);
        }
    }

}


