package com.hrms.backend.controller;

import com.hrms.backend.entities.EmailMessage;
import com.hrms.backend.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/email/")
public class EmailMessageController {

    @Autowired
    private EmailService emailMessageService;

    @GetMapping("/email-messages")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Page<EmailMessage>> getEmailMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String recipientAddress
    ) {
        Page<EmailMessage> emailMessages = emailMessageService.getPaginatedEmailMessages(page, size, recipientAddress);
        return ResponseEntity.ok(emailMessages);
    }

}
