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
     * Schedule to run at midnight on the first day of every month.
     */
   // @Scheduled(cron = "0 0 0 1 * ?")
    @Scheduled(cron = "0 * * * * ?")  // Runs every minute
    public void calculateMonthlySalaries() {
        //salaryService.calculateSalaryForAllEmployees();
        //System.out.println("Monthly salary calculation completed.");
    }
}
