package com.hrms.backend.scheduler;

import com.hrms.backend.services.SalaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalaryScheduler {

    private final SalaryService salaryService;

    /**
     * Schedule to run at 2:00 am on the first day of every month. @Scheduled(cron = "0 0 2 L * ?")
     */
  // @Scheduled(cron = "0 * * * * ?") // sample to run every minute
    @Scheduled(cron = "0 0 2 L * ?")
    public void calculateMonthlySalaries() {
        System.out.println(":::: SALARY CALCULATION STARTED ::::");
        salaryService.calculateSalaryForAllEmployees();
        //System.out.println("Monthly salary calculation completed.");
    }
}
