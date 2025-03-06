package com.hrms.backend.services.impl;


import com.hrms.backend.entities.EmailMessage;
import com.hrms.backend.entities.EmailStatus;
import com.hrms.backend.repository.EmailMessageRepository;
import com.hrms.backend.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmailServiceImpl implements EmailService {
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private EmailMessageRepository emailMessageRepository;

    public boolean sendEmail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        try {
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    @Override
    public void processEmail() {
        List<EmailMessage> emailMessages = emailMessageRepository.findByStatus(EmailStatus.PENDING);
        emailMessages.forEach(emailMessage -> {
            try {
                boolean sent = sendEmail(emailMessage.getRecipientAddress(), emailMessage.getSubject(), emailMessage.getMessage());
                if (sent) {
                    emailMessageRepository.updateEmailStatusById(emailMessage.getId(), EmailStatus.SUCCESS);
                } else {
                    emailMessageRepository.updateEmailStatusById(emailMessage.getId(), EmailStatus.FAILED);
                }
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });

    }
}
