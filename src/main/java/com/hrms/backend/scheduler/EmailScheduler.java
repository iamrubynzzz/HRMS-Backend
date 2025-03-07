package com.hrms.backend.scheduler;

import com.hrms.backend.services.EmailService;
import com.hrms.backend.services.SalaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailScheduler {
    private final EmailService emailService;

    //@Scheduled(cron = "0 * * * * ?") sample to run every minute
    @Scheduled(cron = "0 * * * * ?")
    public void processEmail() {
        emailService.processEmail();
    }
}
