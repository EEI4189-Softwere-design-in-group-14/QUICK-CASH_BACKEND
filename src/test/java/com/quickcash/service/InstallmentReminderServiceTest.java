
package com.quickcash.service;

import com.quickcash.entity.*;
import com.quickcash.repository.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallmentReminderServiceTest {

    @Mock
    private LoanInstallmentRepository installmentRepository;

    @Mock
    private InstallmentReminderLogRepository reminderLogRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InstallmentReminderService service;

    private LoanInstallment createInstallment(
            LocalDate dueDate,
            String paidAmount
    ) {
        User user = new User();
        user.setUserId(50L);

        Borrower borrower = new Borrower();
        borrower.setUser(user);

        Loan loan = new Loan();
        loan.setLoanCode("LN-TEST-REMINDER");
        loan.setBorrower(borrower);
        loan.setStatus(LoanStatus.ACTIVE);

        LoanInstallment installment = new LoanInstallment();
        ReflectionTestUtils.setField(
                installment, "installmentId", 101L
        );

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
    void dueReminderIsSentOnlyOnce() {

        LocalDate today = LocalDate.of(2026, 11, 20);
        var installment =
                createInstallment(today, "0.00");

        when(installmentRepository.findUnpaidDueBetween(
                today,
                today.plusDays(3),
                LoanStatus.ACTIVE
        )).thenReturn(List.of(installment));

        when(reminderLogRepository
                .existsByInstallmentInstallmentIdAndReminderType(
                        101L,
                        NotificationType.PAYMENT_DUE_REMINDER
                )).thenReturn(false, true);

        service.processReminders(today);
        service.processReminders(today);

        verify(notificationService, times(1)).send(
                eq(installment.getLoan().getBorrower().getUser()),
                eq(NotificationType.PAYMENT_DUE_REMINDER),
                anyString(),
                contains("LKR 120.00")
        );

        verify(reminderLogRepository, times(1))
                .save(any(InstallmentReminderLog.class));
    }

    @Test
    void overdueReminderIsSentOnlyOnce() {

        LocalDate today = LocalDate.of(2026, 11, 20);
        var installment = createInstallment(
                today.minusDays(1), "20.00"
        );

        when(installmentRepository.findOverdueUnpaid(
                today,
                LoanStatus.ACTIVE
        )).thenReturn(List.of(installment));

        when(reminderLogRepository
                .existsByInstallmentInstallmentIdAndReminderType(
                        101L,
                        NotificationType.PAYMENT_OVERDUE
                )).thenReturn(false, true);

        service.processReminders(today);
        service.processReminders(today);

        verify(notificationService, times(1)).send(
                eq(installment.getLoan().getBorrower().getUser()),
                eq(NotificationType.PAYMENT_OVERDUE),
                anyString(),
                contains("LKR 100.00")
        );

        verify(reminderLogRepository, times(1))
                .save(any(InstallmentReminderLog.class));
    }

    @Test
    void fullyPaidInstallmentDoesNotCreateReminder() {

        LocalDate today = LocalDate.of(2026, 11, 20);

        var installment =
                createInstallment(today, "120.00");

        when(installmentRepository.findUnpaidDueBetween(
                today,
                today.plusDays(3),
                LoanStatus.ACTIVE
        )).thenReturn(List.of(installment));

        service.processReminders(today);

        verifyNoInteractions(notificationService);

        verify(reminderLogRepository, never())
                .save(any(InstallmentReminderLog.class));
    }
}
