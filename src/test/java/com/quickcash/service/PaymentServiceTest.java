
package com.quickcash.service;

import com.quickcash.dto.PaymentRequest;
import com.quickcash.dto.PaymentResponse;
import com.quickcash.entity.*;
import com.quickcash.repository.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReceiptService receiptService;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private InstallmentAllocationService installmentAllocationService;

    @InjectMocks
    private PaymentService service;

    private User collector(
            Long id,
            UserStatus status
    ) {
        User user = new User();
        user.setUserId(id);
        user.setEmail("collector@test.com");
        user.setFullName("Test Collector");
        user.setRole(Role.COLLECTOR);
        user.setStatus(status);
        return user;
    }

    private Loan loan(Long id, User assigned) {
        User borrowerUser = new User();
        borrowerUser.setFullName("Test Borrower");

        Borrower borrower = new Borrower();
        borrower.setUser(borrowerUser);

        Loan loan = new Loan();
        loan.setLoanId(id);
        loan.setLoanCode("LN-TEST-" + id);
        loan.setBorrower(borrower);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setAssignedCollector(assigned);
        loan.setOutstandingBalance(
                new BigDecimal("100.00")
        );

        return loan;
    }

    private PaymentRequest request(
            Long loanId,
            String amount,
            String transactionId
    ) {
        PaymentRequest request = new PaymentRequest();
        request.setLoanId(loanId);
        request.setAmount(new BigDecimal(amount));
        request.setClientTransactionId(transactionId);
        return request;
    }

    private Payment existingPayment(
            Loan loan,
            User collector
    ) {
        Payment payment = new Payment();
        payment.setPaymentId(50L);
        payment.setPaymentCode("PAY-TEST-50");
        payment.setClientTransactionId("TX-EXISTING");
        payment.setLoan(loan);
        payment.setCollector(collector);
        payment.setAmount(new BigDecimal("10.00"));
        payment.setBalanceBefore(new BigDecimal("100.00"));
        payment.setBalanceAfter(new BigDecimal("90.00"));
        payment.setSource(PaymentSource.ONLINE);
        payment.setStatus(PaymentStatus.ACCEPTED);
        payment.setPaymentDate(
                LocalDateTime.of(2026, 10, 31, 10, 0)
        );
        return payment;
    }

    private void allowCollector(User user, Loan loan) {
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(loanRepository.findByIdForUpdate(
                loan.getLoanId()
        )).thenReturn(Optional.of(loan));
    }

    private void givenExisting(Payment payment) {
        when(paymentRepository.findByClientTransactionId(
                "TX-EXISTING"
        )).thenReturn(Optional.of(payment));
    }

    private void assertConflict(
            PaymentRequest request,
            PaymentSource source
    ) {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.recordPayment(
                        "collector@test.com",
                        request,
                        source
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                error.getStatusCode()
        );

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }

    // TEST 1: Same details must return the original payment.
    @Test
    void identicalRetryReturnsOriginalPayment() {
        User collector = collector(5L, UserStatus.ACTIVE);
        Loan loan = loan(2L, collector);

        allowCollector(collector, loan);
        Payment previous = existingPayment(loan, collector);
        givenExisting(previous);

        PaymentResponse result = service.recordPayment(
                "collector@test.com",
                request(2L, "10.00", "TX-EXISTING"),
                PaymentSource.ONLINE
        );

        assertEquals(50L, result.getPaymentId());
        assertEquals(
                new BigDecimal("100.00"),
                loan.getOutstandingBalance()
        );

        verify(receiptService)
                .createReceiptIfMissing(previous);

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(installmentAllocationService, never())
                .allocatePayment(any(), any());

        verifyNoInteractions(
                auditService,
                notificationService
        );
    }

    // TEST 2: Same ID with another amount must be rejected.
    @Test
    void changedAmountReturns409() {
        User collector = collector(5L, UserStatus.ACTIVE);
        Loan loan = loan(2L, collector);

        allowCollector(collector, loan);
        givenExisting(existingPayment(loan, collector));

        assertConflict(
                request(2L, "20.00", "TX-EXISTING"),
                PaymentSource.ONLINE
        );
    }

    // TEST 3: Same ID with another loan must be rejected.
    @Test
    void changedLoanReturns409() {
        User collector = collector(5L, UserStatus.ACTIVE);

        Loan requestedLoan = loan(2L, collector);
        Loan originalLoan = loan(1L, collector);

        allowCollector(collector, requestedLoan);
        givenExisting(existingPayment(
                originalLoan, collector
        ));

        assertConflict(
                request(2L, "10.00", "TX-EXISTING"),
                PaymentSource.ONLINE
        );
    }

    // TEST 4: Same ID with another source must be rejected.
    @Test
    void changedPaymentSourceReturns409() {
        User collector = collector(5L, UserStatus.ACTIVE);
        Loan loan = loan(2L, collector);

        allowCollector(collector, loan);
        givenExisting(existingPayment(loan, collector));

        assertConflict(
                request(2L, "10.00", "TX-EXISTING"),
                PaymentSource.OFFLINE_SYNC
        );
    }

    // TEST 5: An unassigned collector cannot pay this loan.
    @Test
    void unassignedCollectorReturns404() {
        User collector = collector(5L, UserStatus.ACTIVE);
        User other = collector(7L, UserStatus.ACTIVE);

        allowCollector(collector, loan(2L, other));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.recordPayment(
                        "collector@test.com",
                        request(2L, "1.00", "TX-NEW"),
                        PaymentSource.ONLINE
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        verifyNoInteractions(paymentRepository);
    }

    // TEST 6: Suspended collectors cannot collect payments.
    @Test
    void suspendedCollectorReturns403() {
        User collector = collector(
                5L, UserStatus.SUSPENDED
        );

        when(userRepository.findByEmail(
                "collector@test.com"
        )).thenReturn(Optional.of(collector));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.recordPayment(
                        "collector@test.com",
                        request(2L, "1.00", "TX-NEW"),
                        PaymentSource.ONLINE
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                error.getStatusCode()
        );

        verifyNoInteractions(
                loanRepository,
                paymentRepository
        );
    }

    // TEST 7: New payment updates the balance once.
    @Test
    void newPaymentUpdatesLoanOnce() {
        User collector = collector(5L, UserStatus.ACTIVE);
        Loan loan = loan(2L, collector);

        allowCollector(collector, loan);

        when(paymentRepository.findByClientTransactionId(
                "TX-NEW"
        )).thenReturn(Optional.empty());

        when(paymentRepository.save(
                any(Payment.class)
        )).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(51L);
            return payment;
        });

        PaymentResponse result = service.recordPayment(
                "collector@test.com",
                request(2L, "1.00", "TX-NEW"),
                PaymentSource.ONLINE
        );

        assertEquals(51L, result.getPaymentId());

        assertEquals(
                new BigDecimal("99.00"),
                loan.getOutstandingBalance()
        );

        verify(loanRepository, times(1))
                .save(loan);

        verify(paymentRepository, times(1))
                .save(any(Payment.class));

        verify(installmentAllocationService)
                .allocatePayment(
                        eq(loan),
                        eq(new BigDecimal("1.00"))
                );

        verify(receiptService)
                .createReceiptIfMissing(any(Payment.class));

        verify(auditService, times(1))
                .record(
                        eq(collector),
                        eq(AuditAction.PAYMENT_RECORDED),
                        eq("PAYMENT"),
                        eq(51L),
                        anyString(),
                        anyString()
                );
    }

    // TEST 8: The payment method must use READ_COMMITTED.
    @Test
    void paymentUsesReadCommittedIsolation()
            throws Exception {

        var method = PaymentService.class.getMethod(
                "recordPayment",
                String.class,
                PaymentRequest.class,
                PaymentSource.class
        );

        Transactional annotation =
                method.getAnnotation(Transactional.class);

        assertNotNull(annotation);
        assertEquals(
                Isolation.READ_COMMITTED,
                annotation.isolation()
        );
    }
}
