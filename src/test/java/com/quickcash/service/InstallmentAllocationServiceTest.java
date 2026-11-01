
package com.quickcash.service;

import com.quickcash.entity.Loan;
import com.quickcash.entity.LoanInstallment;
import com.quickcash.repository.LoanInstallmentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallmentAllocationServiceTest {

    @Mock
    private LoanInstallmentRepository installmentRepository;

    @InjectMocks
    private InstallmentAllocationService allocationService;

    private Loan createLoan(String planType) {

        Loan loan = new Loan();
        loan.setLoanId(100L);
        loan.setLoanCode("LN-TEST-100");
        loan.setRepaymentPlanType(planType);

        return loan;
    }

    private LoanInstallment createInstallment(
            int number,
            String principal,
            String interest,
            String paid
    ) {

        LoanInstallment installment = new LoanInstallment();

        installment.setInstallmentNumber(number);
        installment.setPrincipalDue(new BigDecimal(principal));
        installment.setInterestDue(new BigDecimal(interest));
        installment.setPaidAmount(new BigDecimal(paid));

        return installment;
    }

    // Test 1: Full daily installment payment
    @Test
    void shouldAllocatePrincipalAndInterest() {

        Loan loan = createLoan("DAILY_FIXED_TOTAL");

        LoanInstallment installment = createInstallment(
                1, "100.00", "20.00", "0.00"
        );

        when(installmentRepository
                .findByLoanLoanIdOrderByInstallmentNumberAsc(100L))
                .thenReturn(List.of(installment));

        allocationService.allocatePayment(
                loan, new BigDecimal("120.00")
        );

        assertEquals(
                new BigDecimal("120.00"),
                installment.getPaidAmount()
        );

        verify(installmentRepository).saveAll(any());
    }

    // Test 2: Partial payment
    @Test
    void shouldHandlePartialPayment() {

        Loan loan = createLoan("DAILY_FIXED_TOTAL");

        LoanInstallment installment = createInstallment(
                1, "100.00", "20.00", "0.00"
        );

        when(installmentRepository
                .findByLoanLoanIdOrderByInstallmentNumberAsc(100L))
                .thenReturn(List.of(installment));

        allocationService.allocatePayment(
                loan, new BigDecimal("80.00")
        );

        assertEquals(
                new BigDecimal("80.00"),
                installment.getPaidAmount()
        );
    }

    // Test 3: Payment carries over to next installment
    @Test
    void shouldAllocateRemainingToNextInstallment() {

        Loan loan = createLoan("DAILY_FIXED_TOTAL");

        LoanInstallment first = createInstallment(
                1, "100.00", "20.00", "80.00"
        );

        LoanInstallment second = createInstallment(
                2, "100.00", "20.00", "0.00"
        );

        when(installmentRepository
                .findByLoanLoanIdOrderByInstallmentNumberAsc(100L))
                .thenReturn(List.of(first, second));

        allocationService.allocatePayment(
                loan, new BigDecimal("100.00")
        );

        assertEquals(
                new BigDecimal("120.00"),
                first.getPaidAmount()
        );

        assertEquals(
                new BigDecimal("60.00"),
                second.getPaidAmount()
        );
    }

    // Test 4: Daily loan without schedule
    @Test
    void shouldRejectDailyLoanWithoutSchedule() {

        Loan loan = createLoan("DAILY_FIXED_TOTAL");

        when(installmentRepository
                .findByLoanLoanIdOrderByInstallmentNumberAsc(100L))
                .thenReturn(List.of());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> allocationService.allocatePayment(
                        loan, new BigDecimal("120.00")
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        verify(installmentRepository, never())
                .saveAll(any());
    }

    // Test 5: Preserve legacy monthly loan behavior
    @Test
    void shouldSupportLegacyMonthlyLoanWithoutSchedule() {

        Loan loan = createLoan("MONTHLY_PRINCIPAL_ONLY");

        when(installmentRepository
                .findByLoanLoanIdOrderByInstallmentNumberAsc(100L))
                .thenReturn(List.of());

        assertDoesNotThrow(
                () -> allocationService.allocatePayment(
                        loan, new BigDecimal("100.00")
                )
        );

        verify(installmentRepository, never())
                .saveAll(any());
    }

    // Test 6: Reject payment above unpaid schedule total
    @Test
    void shouldRejectExcessPayment() {

        Loan loan = createLoan("DAILY_FIXED_TOTAL");

        LoanInstallment installment = createInstallment(
                1, "100.00", "20.00", "0.00"
        );

        when(installmentRepository
                .findByLoanLoanIdOrderByInstallmentNumberAsc(100L))
                .thenReturn(List.of(installment));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> allocationService.allocatePayment(
                        loan, new BigDecimal("150.00")
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        verify(installmentRepository, never())
                .saveAll(any());
    }
}
