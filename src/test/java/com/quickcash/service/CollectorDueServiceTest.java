
package com.quickcash.service;
import java.math.BigDecimal;

import com.quickcash.entity.*;
import com.quickcash.repository.LoanInstallmentRepository;
import com.quickcash.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectorDueServiceTest {

    @Mock
    private LoanInstallmentRepository installmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CollectorDueService collectorDueService;

    // TEST 1: Active collector can access assigned worklist
    @Test
    void testActiveCollectorUsesCorrectId() {

        User collector = mock(User.class);

        LocalDate date = LocalDate.of(2026, 11, 17);

        when(userRepository.findByEmail("collector@test.com"))
                .thenReturn(Optional.of(collector));

        when(collector.getRole())
                .thenReturn(Role.COLLECTOR);

        when(collector.getStatus())
                .thenReturn(UserStatus.ACTIVE);

        when(collector.getUserId())
                .thenReturn(42L);

        when(installmentRepository.findDueForCollector(
                42L,
                LoanStatus.ACTIVE,
                date
        )).thenReturn(List.of());

        var result = collectorDueService.getDueInstallments(
                "collector@test.com",
                date
        );

        assertTrue(result.isEmpty());

        verify(installmentRepository).findDueForCollector(
                42L,
                LoanStatus.ACTIVE,
                date
        );
    }

    // TEST 2: Manager cannot access collector worklist
    @Test
    void testManagerCannotAccessCollectorDueWorklist() {

        User manager = mock(User.class);

        when(userRepository.findByEmail("manager@test.com"))
                .thenReturn(Optional.of(manager));

        when(manager.getRole())
                .thenReturn(Role.MANAGER);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> collectorDueService.getDueInstallments(
                        "manager@test.com",
                        LocalDate.of(2026, 11, 17)
                )
        );

        assertEquals(
                403,
                exception.getStatusCode().value()
        );

        verifyNoInteractions(installmentRepository);
    }


    @Test
    void testDueAndOverdueSummary() {

        LocalDate date = LocalDate.of(2026, 11, 17);

        User collector = mock(User.class);

        when(userRepository.findByEmail("collector@test.com"))
                .thenReturn(Optional.of(collector));

        when(collector.getRole())
                .thenReturn(Role.COLLECTOR);

        when(collector.getStatus())
                .thenReturn(UserStatus.ACTIVE);

        when(collector.getUserId())
                .thenReturn(42L);

        User borrowerUser = mock(User.class);
        Borrower borrower = mock(Borrower.class);
        Loan loan = mock(Loan.class);

        when(loan.getLoanId()).thenReturn(10L);
        when(loan.getLoanCode()).thenReturn("QC-010");
        when(loan.getBorrower()).thenReturn(borrower);

        when(borrower.getUser()).thenReturn(borrowerUser);
        when(borrowerUser.getFullName())
                .thenReturn("Test Borrower");
        when(borrowerUser.getPhone())
                .thenReturn("0770000000");

        LoanInstallment due = new LoanInstallment();
        due.setLoan(loan);
        due.setInstallmentNumber(1);
        due.setDueDate(date);
        due.setPrincipalDue(new BigDecimal("1000.00"));
        due.setPaidAmount(new BigDecimal("200.00"));

        LoanInstallment overdue = new LoanInstallment();
        overdue.setLoan(loan);
        overdue.setInstallmentNumber(2);
        overdue.setDueDate(date.minusDays(2));
        overdue.setPrincipalDue(new BigDecimal("600.00"));
        overdue.setPaidAmount(new BigDecimal("150.00"));

        when(installmentRepository.findDueForCollector(
                42L, LoanStatus.ACTIVE, date
        )).thenReturn(List.of(due, overdue));

        var summary = collectorDueService.getDueSummary(
                "collector@test.com", date
        );

        assertEquals(date, summary.asOfDate());
        assertEquals(1L, summary.dueOnDateCount());
        assertEquals(1L, summary.overdueCount());

        assertEquals(
                0,
                new BigDecimal("1250.00")
                        .compareTo(summary.totalRemainingDue())
        );
    }

}
