
package com.quickcash.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class InstallmentReminderScheduler {

    private final InstallmentReminderService reminderService;

    public InstallmentReminderScheduler(
            InstallmentReminderService reminderService
    ) {
        this.reminderService = reminderService;
    }

    @Scheduled(
            cron = "0 0 8 * * *",
            zone = "Asia/Colombo"
    )
    public void checkReminders() {

        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Colombo")
        );

        reminderService.processReminders(today);
    }
}

