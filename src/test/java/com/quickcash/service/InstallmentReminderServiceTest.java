
package com.quickcash.service;

import com.quickcash.entity.NotificationType;
import com.quickcash.repository.InstallmentReminderLogRepository;
import com.quickcash.repository.NotificationRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
class InstallmentReminderServiceTest {

    @Autowired
    private InstallmentReminderService reminderService;

    @Autowired
    private InstallmentReminderLogRepository reminderLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void testDueAndOverdueReminders() {

        Long installmentId = 1L;
        Long borrowerUserId = 6L;

        // STEP 1: Simulate November 20, 2026.
        reminderService.processReminders(
                LocalDate.of(2026, 11, 20)
        );

        boolean dueReminderCreated =
                reminderLogRepository
                        .existsByInstallmentInstallmentIdAndReminderType(
                                installmentId,
                                NotificationType.PAYMENT_DUE_REMINDER
                        );

        assertTrue(
                dueReminderCreated,
                "Due reminder was not created"
        );

        // STEP 2: Run the same check again.
        // There must be no duplicate reminder.
        reminderService.processReminders(
                LocalDate.of(2026, 11, 20)
        );

        long dueCount = reminderLogRepository.findAll()
                .stream()
                .filter(log ->
                        log.getInstallment()
                                .getInstallmentId()
                                .equals(installmentId)
                                && log.getReminderType()
                                == NotificationType.PAYMENT_DUE_REMINDER
                )
                .count();

        assertEquals(
                1L,
                dueCount,
                "Duplicate due reminder detected"
        );

        // STEP 3: Simulate November 24, 2026.
        reminderService.processReminders(
                LocalDate.of(2026, 11, 24)
        );

        boolean overdueReminderCreated =
                reminderLogRepository
                        .existsByInstallmentInstallmentIdAndReminderType(
                                installmentId,
                                NotificationType.PAYMENT_OVERDUE
                        );

        assertTrue(
                overdueReminderCreated,
                "Overdue reminder was not created"
        );

        // STEP 4: Verify notification records.
        var notifications =
                notificationRepository
                        .findByRecipient_UserIdOrderByCreatedAtDesc(
                                borrowerUserId
                        );

        assertTrue(
                notifications.stream().anyMatch(
                        n -> n.getType()
                                == NotificationType.PAYMENT_DUE_REMINDER
                ),
                "Borrower due notification missing"
        );

        assertTrue(
                notifications.stream().anyMatch(
                        n -> n.getType()
                                == NotificationType.PAYMENT_OVERDUE
                ),
                "Borrower overdue notification missing"
        );

        System.out.println(
                "QUICKCASH REMINDER TEST PASSED"
        );

        System.out.println(
                "Due reminder created successfully"
        );

        System.out.println(
                "Duplicate reminder prevented"
        );

        System.out.println(
                "Overdue reminder created successfully"
        );

        System.out.println(
                "Borrower notifications verified"
        );
    }
}
