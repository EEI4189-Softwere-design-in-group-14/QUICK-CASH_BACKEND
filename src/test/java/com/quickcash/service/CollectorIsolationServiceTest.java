
package com.quickcash.service;

import com.quickcash.dto.PaymentRequest;
import com.quickcash.entity.*;
import com.quickcash.repository.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectorIsolationServiceTest {

    private static final String COLLECTOR_A_EMAIL =
            "collector-a@test.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private LoanFollowUpRepository followUpRepository;

    @Mock
    private ReceiptService receiptService;

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private InstallmentAllocationService installmentAllocationService;

    @InjectMocks
    private PaymentService paymentService;

    @InjectMocks
    private LoanFollowUpService followUpService;

    private User collector(Long id, String email) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        user.setFullName("Test Collector");
        user.setRole(Role.COLLECTOR);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Loan loanOwnedBy(User collectorB) {
        Loan loan = new Loan();
        loan.setLoanId(9L);
        loan.setLoanCode("LN-SECURITY-9");
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setAssignedCollector(collectorB);
        loan.setOutstandingBalance(
                new BigDecimal("1000.00")
        );
        return loan;
    }

    private void setupCollector(User collectorA) {
        when(userRepository.findByEmail(COLLECTOR_A_EMAIL))
                .thenReturn(Optional.of(collectorA));
    }

    // Test 1 - Cannot view another collector's payments
    @Test
    void collectorCannotReadOtherCollectorsPayments() {

        User collectorA =
                collector(5L, COLLECTOR_A_EMAIL);

        User collectorB =
                collector(7L, "collector-b@test.com");

        setupCollector(collectorA);

        when(loanRepository.findById(9L))
                .thenReturn(
                        Optional.of(loanOwnedBy(collectorB))
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> paymentService.getLoanPayments(
                        9L,
                        COLLECTOR_A_EMAIL
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        verifyNoInteractions(paymentRepository);
    }

    // Test 2 - Cannot view another collector's follow-ups
    @Test
    void collectorCannotReadOtherCollectorsFollowUps() {

        User collectorA =
                collector(5L, COLLECTOR_A_EMAIL);

        User collectorB =
                collector(7L, "collector-b@test.com");

        setupCollector(collectorA);

        when(loanRepository.findById(9L))
                .thenReturn(
                        Optional.of(loanOwnedBy(collectorB))
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> followUpService.getForCollector(
                        9L,
                        COLLECTOR_A_EMAIL
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        verifyNoInteractions(followUpRepository);
    }

    // Test 3 - Cannot pay another collector's loan
    @Test
    void collectorCannotRecordPaymentForOtherLoan() {

        User collectorA =
                collector(5L, COLLECTOR_A_EMAIL);

        User collectorB =
                collector(7L, "collector-b@test.com");

        setupCollector(collectorA);

        when(loanRepository.findByIdForUpdate(9L))
                .thenReturn(
                        Optional.of(loanOwnedBy(collectorB))
                );

        PaymentRequest request = new PaymentRequest();
        request.setLoanId(9L);
        request.setAmount(new BigDecimal("1.00"));
        request.setClientTransactionId(
                "QC-UNAUTHORIZED-TEST"
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> paymentService.recordPayment(
                        COLLECTOR_A_EMAIL,
                        request,
                        PaymentSource.ONLINE
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        verifyNoInteractions(
                paymentRepository,
                receiptService,
                installmentAllocationService,
                auditService,
                notificationService
        );
    }
}
