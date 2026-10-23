
package com.quickcash.service;

import com.quickcash.entity.*;
import com.quickcash.repository.InstallmentReminderLogRepository;
import com.quickcash.repository.LoanInstallmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class InstallmentReminderService {

    private final LoanInstallmentRepository installmentRepository;
    private final InstallmentReminderLogRepository reminderLogRepository;
    private final NotificationService notificationService;

    public InstallmentReminderService(
            LoanInstallmentRepository installmentRepository,
            InstallmentReminderLogRepository reminderLogRepository,
            NotificationService notificationService
    ) {
        this.installmentRepository = installmentRepository;
        this.reminderLogRepository = reminderLogRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void processReminders(LocalDate today) {

        // Find unpaid installments due within 3 days.
        List<LoanInstallment> upcoming =
                installmentRepository.findUnpaidDueBetween(
                        today,
                        today.plusDays(3),
                        LoanStatus.ACTIVE
                );

        for (LoanInstallment installment : upcoming) {
            createReminder(
                    installment,
                    NotificationType.PAYMENT_DUE_REMINDER
            );
        }

        // Find unpaid installments past their due date.
        List<LoanInstallment> overdue =
                installmentRepository.findOverdueUnpaid(
                        today,
                        LoanStatus.ACTIVE
                );

        for (LoanInstallment installment : overdue) {
            createReminder(
                    installment,
                    NotificationType.PAYMENT_OVERDUE
            );
        }
    }

    private void createReminder(
            LoanInstallment installment,
            NotificationType type
    ) {

        Long installmentId = installment.getInstallmentId();

        // Skip if this reminder type already exists.
        boolean alreadySent = reminderLogRepository
                .existsByInstallmentInstallmentIdAndReminderType(
                        installmentId,
                        type
                );

        if (alreadySent) {
            return;
        }

        BigDecimal remaining =
                installment.getPrincipalDue().subtract(
                        installment.getPaidAmount()
                );

        // Never notify for fully paid installments.
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Loan loan = installment.getLoan();

        String title;
        String message;

        if (type == NotificationType.PAYMENT_DUE_REMINDER) {

            title = "Installment Payment Reminder";

            message = "Your installment #"
                    + installment.getInstallmentNumber()
                    + " for loan "
                    + loan.getLoanCode()
                    + " is due on "
                    + installment.getDueDate()
                    + ". Remaining amount: LKR "
                    + remaining.toPlainString();

        } else if (type == NotificationType.PAYMENT_OVERDUE) {

            title = "Overdue Installment";

            message = "Your installment #"
                    + installment.getInstallmentNumber()
                    + " for loan "
                    + loan.getLoanCode()
                    + " was due on "
                    + installment.getDueDate()
                    + ". Outstanding amount: LKR "
                    + remaining.toPlainString();

        } else {
            throw new IllegalArgumentException(
                    "Unsupported reminder type"
            );
        }

        notificationService.send(
                loan.getBorrower().getUser(),
                type,
                title,
                message
        );

        InstallmentReminderLog reminderLog =
                new InstallmentReminderLog();

        reminderLog.setInstallment(installment);
        reminderLog.setReminderType(type);

        reminderLogRepository.save(reminderLog);
    }
}

