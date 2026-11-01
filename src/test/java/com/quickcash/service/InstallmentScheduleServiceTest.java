
package com.quickcash.service;

import com.quickcash.entity.Loan;
import com.quickcash.entity.LoanInstallment;
import com.quickcash.repository.LoanInstallmentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallmentScheduleServiceTest {

    @Mock
    private LoanInstallmentRepository installmentRepository;

    @InjectMocks
    private InstallmentScheduleService scheduleService;

    @Captor
    private ArgumentCaptor<List<LoanInstallment>>
            installmentsCaptor;

    private Loan createTestLoan() {

        Loan loan = new Loan();

        loan.setLoanId(100L);
        loan.setRepaymentPlanType("DAILY_FIXED_TOTAL");
        loan.setApprovedAmount(
                new BigDecimal("10000.00")
        );

        loan.setTotalRepaymentAmount(
                new BigDecimal("12000.00")
        );

        loan.setRepaymentDays(100);

        return loan;
    }

    @Test
    void shouldCreate100DailyInstallments() {

        Loan loan = createTestLoan();

        LocalDate disbursementDate =
                LocalDate.of(2026, 11, 1);

        scheduleService.createDailySchedule(
                loan,
                disbursementDate
        );

        verify(installmentRepository)
                .saveAll(installmentsCaptor.capture());

        List<LoanInstallment> installments =
                installmentsCaptor.getValue();

        // Check installment count
        assertEquals(100, installments.size());

        // Check first due date
        assertEquals(
                LocalDate.of(2026, 11, 2),
                installments.get(0).getDueDate()
        );

        // Check last due date
        assertEquals(
                disbursementDate.plusDays(100),
                installments.get(99).getDueDate()
        );

        // Calculate total principal
        BigDecimal totalPrincipal =
                installments.stream()
                        .map(LoanInstallment::getPrincipalDue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total interest
        BigDecimal totalInterest =
                installments.stream()
                        .map(LoanInstallment::getInterestDue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Verify principal amount
        assertEquals(
                0,
                new BigDecimal("10000.00")
                        .compareTo(totalPrincipal)
        );

        // Verify interest amount
        assertEquals(
                0,
                new BigDecimal("2000.00")
                        .compareTo(totalInterest)
        );

        // Verify total repayment
        BigDecimal totalRepayment =
                totalPrincipal.add(totalInterest);

        assertEquals(
                0,
                new BigDecimal("12000.00")
                        .compareTo(totalRepayment)
        );
    }

    @Test
    void shouldPreventDuplicateSchedule() {

        Loan loan = createTestLoan();

        when(installmentRepository.existsByLoanLoanId(100L))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> scheduleService.createDailySchedule(
                        loan,
                        LocalDate.of(2026, 11, 1)
                )
        );

        verify(installmentRepository, never())
                .saveAll(any());
    }
}
