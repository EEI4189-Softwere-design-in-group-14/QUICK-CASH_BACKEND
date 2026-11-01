
package com.quickcash.service;

import com.quickcash.entity.*;
import com.quickcash.repository.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallmentReminderInterestTest {

    @Mock
    private LoanInstallmentRepository installmentRepository;

    @Mock
    private InstallmentReminderLogRepository reminderLogRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InstallmentReminderService reminderService;

    private LoanInstallment createInstallment(
            LocalDate dueDate,
            String paidAmount
    ) {
        User borrowerUser = new User();
        borrowerUser.setUserId(50L);

        Borrower borrower = new Borrower();
        borrower.setUser(borrowerUser);

        Loan loan = new Loan();
        loan.setLoanCode("LN-M25-TEST");
        loan.setBorrower(borrower);

        LoanInstallment installment = new LoanInstallment();
        installment.setLoan(loan);
        installment.setInstallmentNumber(1);
        installment.setDueDate(dueDate);

        installment.setPrincipalDue(
                new BigDecimal("100.00")
        );
        installment.setInterestDue(
                new BigDecimal("20.00")
        );
        installment.setPaidAmount(
                new BigDecimal(paidAmount)
        );

        return installment;
    }

    @Test
    void dueReminderShouldIncludeRemainingInterest() {

        LocalDate today = LocalDate.of(2026, 11, 1);

        LoanInstallment installment =
                createInstallment(today.plusDays(1), "80.00");

        when(installmentRepository.findUnpaidDueBetween(
                today,
                today.plusDays(3),
                LoanStatus.ACTIVE
        )).thenReturn(List.of(installment));

        reminderService.processReminders(today);

        ArgumentCaptor<String> messageCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(notificationService).send(
                any(User.class),
                eq(NotificationType.PAYMENT_DUE_REMINDER),
                anyString(),
                messageCaptor.capture()
        );

        assertTrue(
                messageCaptor.getValue()
                        .contains("LKR 40.00")
        );

        verify(reminderLogRepository).save(
                any(InstallmentReminderLog.class)
        );
    }

    @Test
    void overdueReminderShouldIncludeRemainingInterest() {

        LocalDate today = LocalDate.of(2026, 11, 1);

        LoanInstallment installment =
                createInstallment(today.minusDays(1), "80.00");

        when(installmentRepository.findOverdueUnpaid(
                today,
                LoanStatus.ACTIVE
        )).thenReturn(List.of(installment));

        reminderService.processReminders(today);

        ArgumentCaptor<String> messageCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(notificationService).send(
                any(User.class),
                eq(NotificationType.PAYMENT_OVERDUE),
                anyString(),
                messageCaptor.capture()
        );

        assertTrue(
                messageCaptor.getValue()
                        .contains("LKR 40.00")
        );
    }

    @Test
    void fullyPaidInstallmentShouldNotSendReminder() {

        LocalDate today = LocalDate.of(2026, 11, 1);

        LoanInstallment installment =
                createInstallment(today.plusDays(1), "120.00");

        when(installmentRepository.findUnpaidDueBetween(
                today,
                today.plusDays(3),
                LoanStatus.ACTIVE
        )).thenReturn(List.of(installment));

        reminderService.processReminders(today);

        verifyNoInteractions(notificationService);

        verify(reminderLogRepository, never()).save(
                any(InstallmentReminderLog.class)
        );
    }
}
