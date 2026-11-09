
package com.quickcash.service;

import com.quickcash.entity.*;
import com.quickcash.repository.*;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/quickcash_test_db",
        "spring.datasource.username=root",
        "spring.datasource.password=${DB_PASSWORD}",
        "spring.jpa.hibernate.ddl-auto=update",
        "jwt.secret=quickcash-integration-test-secret-key-only-2026",
        "jwt.expiration=86400000",
        "quickcash.sms.dispatch-enabled=false"
})
@ActiveProfiles("integration")
@Transactional
@Rollback

@EnabledIfEnvironmentVariable(
        named = "QUICKCASH_DB_TESTS",
        matches = "true"
)
class QuickCashDatabaseIntegrationTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private SmsNotificationRepository smsRepository;

    @Autowired
    private LoanInstallmentRepository installmentRepository;

    record TestData(
            Loan loan,
            User borrowerUser,
            User collector
    ) {}

    @BeforeEach
    void verifyTestDatabase() {
        String database = (String) em
                .createNativeQuery("SELECT DATABASE()")
                .getSingleResult();

        assertEquals(
                "quickcash_test_db",
                database,
                "STOP: Refusing to use a non-test database!"
        );
    }

    private TestData createTestData() {

        String id = String.format(
                "%09d",
                Math.floorMod(
                        UUID.randomUUID()
                                .getLeastSignificantBits(),
                        1_000_000_000L
                )
        );

        User borrowerUser = new User();
        borrowerUser.setFullName("Integration Test Borrower");
        borrowerUser.setEmail("borrower-" + id + "@example.test");
        borrowerUser.setPhone("07" + id);
        borrowerUser.setPassword("test-only");
        borrowerUser.setRole(Role.BORROWER);
        borrowerUser.setStatus(UserStatus.ACTIVE);
        em.persist(borrowerUser);

        User collector = new User();
        collector.setFullName("Integration Test Collector");
        collector.setEmail("collector-" + id + "@example.test");
        collector.setPhone("08" + id);
        collector.setPassword("test-only");
        collector.setRole(Role.COLLECTOR);
        collector.setStatus(UserStatus.ACTIVE);
        em.persist(collector);

        Borrower borrower = new Borrower();
        borrower.setUser(borrowerUser);
        borrower.setBorrowerCode("BR-T" + id);
        borrower.setNic("T" + id);
        borrower.setAddress("Test address");
        borrower.setStatus(BorrowerStatus.APPROVED);
        em.persist(borrower);

        Loan loan = new Loan();
        loan.setLoanCode("LN-T" + id);
        loan.setBorrower(borrower);
        loan.setRequestedAmount(new BigDecimal("1000.00"));
        loan.setApprovedAmount(new BigDecimal("1000.00"));
        loan.setOutstandingBalance(new BigDecimal("1100.00"));
        loan.setPurpose("Database integration test");
        loan.setRepaymentMonths(1);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setAssignedCollector(collector);
        em.persist(loan);

        em.flush();

        return new TestData(loan, borrowerUser, collector);
    }

    @Test
    void collectorDueQueryIncludesUnpaidInterest() {

        TestData data = createTestData();

        LoanInstallment installment = new LoanInstallment();
        installment.setLoan(data.loan());
        installment.setInstallmentNumber(1);
        installment.setDueDate(LocalDate.now().minusDays(1));
        installment.setPrincipalDue(
                new BigDecimal("100.00")
        );
        installment.setInterestDue(
                new BigDecimal("20.00")
        );
        installment.setPaidAmount(
                new BigDecimal("100.00")
        );

        em.persist(installment);
        em.flush();
        em.clear();

        var results = installmentRepository.findDueForCollector(
                data.collector().getUserId(),
                LoanStatus.ACTIVE,
                LocalDate.now()
        );

        assertTrue(
                results.stream().anyMatch(item ->
                        item.getInstallmentId()
                                .equals(installment.getInstallmentId())
                ),
                "Installment with unpaid interest must appear"
        );
    }

    @Test
    void oldSmsIsFlaggedForManualReview() {

        TestData data = createTestData();

        Payment payment = new Payment();
        payment.setLoan(data.loan());
        payment.setCollector(data.collector());
        payment.setPaymentCode("PAY-T" + UUID.randomUUID()
                .toString().substring(0, 8));
        payment.setClientTransactionId(
                "INTEGRATION-" + UUID.randomUUID()
        );
        payment.setAmount(new BigDecimal("110.00"));
        payment.setBalanceBefore(
                new BigDecimal("1100.00")
        );
        payment.setBalanceAfter(
                new BigDecimal("990.00")
        );
        payment.setStatus(PaymentStatus.ACCEPTED);
        payment.setSource(PaymentSource.ONLINE);
        payment.setPaymentDate(LocalDateTime.now());
        em.persist(payment);

        SmsNotification sms = new SmsNotification();
        sms.setPayment(payment);
        sms.setRecipientPhone(data.borrowerUser().getPhone());
        sms.setMessageBody("Integration test SMS");
        sms.setStatus(SmsNotificationStatus.PROCESSING);
        sms.setProcessingStartedAt(
                LocalDateTime.now().minusMinutes(45)
        );

        em.persist(sms);
        em.flush();

        Long smsId = sms.getSmsId();
        Long loanId = data.loan().getLoanId();

        em.clear();

        int recovered =
                smsRepository.markStuckProcessingForReview(
                        SmsNotificationStatus.PROCESSING,
                        SmsNotificationStatus.FAILED,
                        LocalDateTime.now().minusMinutes(30)
                );

        em.clear();

        SmsNotification updated =
                smsRepository.findById(smsId).orElseThrow();

        assertEquals(1, recovered);
        assertEquals(
                SmsNotificationStatus.FAILED,
                updated.getStatus()
        );

        assertTrue(
                updated.getLastError()
                        .contains("manual review")
        );

        Loan unchangedLoan = em.find(Loan.class, loanId);

        assertEquals(
                0,
                new BigDecimal("1100.00").compareTo(
                        unchangedLoan.getOutstandingBalance()
                )
        );
    }
}
