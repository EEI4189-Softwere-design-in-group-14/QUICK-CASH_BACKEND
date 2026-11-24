package com.quickcash.service;

import com.quickcash.dto.OtpVerifyRequest;
import com.quickcash.entity.*;
import com.quickcash.repository.DisbursementRepository;
import com.quickcash.repository.LoanRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/quickcash_test_db",
        "spring.datasource.username=root",
        "spring.datasource.password=${DB_PASSWORD}",
        "spring.jpa.hibernate.ddl-auto=update",
        "jwt.secret=quickcash-otp-integration-test-secret-key-2026",
        "jwt.expiration=86400000",
        "quickcash.sms.dispatch-enabled=false",
        "quickcash.sms.provider=mock"
})
@ActiveProfiles("integration")
@EnabledIfEnvironmentVariable(
        named = "QUICKCASH_DB_TESTS",
        matches = "true"
)
class OtpDatabaseIntegrationTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private DisbursementService service;

    @Autowired
    private DisbursementRepository disbursementRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Never send real SMS during integration testing.
    @MockitoBean
    private OtpDeliveryService otpDeliveryService;

    // Isolate OTP transaction and locking behaviour.
    @MockitoBean
    private InstallmentScheduleService installmentScheduleService;

    @MockitoBean
    private AuditService auditService;

    private Long loanId;
    private Long borrowerId;
    private Long borrowerUserId;
    private Long managerUserId;
    private Long disbursementId;

    private String managerEmail;

    private static final String TEST_OTP = "123456";
    private static final String WRONG_OTP = "654321";

    // Verify the database before creating test records.
    @BeforeEach
    void setUp() {

        transactions.executeWithoutResult(status -> {

            String database = (String) em
                    .createNativeQuery("SELECT DATABASE()")
                    .getSingleResult();

            assertEquals(
                    "quickcash_test_db",
                    database,
                    "STOP: Refusing to use a non-test database!"
            );
        });

        createTestData();
    }

    // Create isolated test users and an approved loan.
    private void createTestData() {

        transactions.executeWithoutResult(status -> {

            String id = String.format(
                    "%09d",
                    Math.floorMod(
                            UUID.randomUUID()
                                    .getLeastSignificantBits(),
                            1_000_000_000L
                    )
            );

            User borrowerUser = new User();

            borrowerUser.setFullName(
                    "OTP Integration Test Borrower"
            );

            borrowerUser.setEmail(
                    "otp-borrower-" + id + "@example.test"
            );

            borrowerUser.setPhone("07" + id);
            borrowerUser.setPassword("test-only");
            borrowerUser.setRole(Role.BORROWER);
            borrowerUser.setStatus(UserStatus.ACTIVE);

            em.persist(borrowerUser);

            User manager = new User();

            manager.setFullName(
                    "OTP Integration Test Manager"
            );

            managerEmail =
                    "otp-manager-" + id + "@example.test";

            manager.setEmail(managerEmail);
            manager.setPhone("08" + id);
            manager.setPassword("test-only");
            manager.setRole(Role.MANAGER);
            manager.setStatus(UserStatus.ACTIVE);

            em.persist(manager);

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

            loan.setRequestedAmount(
                    new BigDecimal("1000.00")
            );

            loan.setApprovedAmount(
                    new BigDecimal("1000.00")
            );

            loan.setOutstandingBalance(
                    BigDecimal.ZERO
            );

            loan.setPurpose("OTP integration test");
            loan.setRepaymentMonths(1);

            loan.setRepaymentPlanType(
                    "MONTHLY_PRINCIPAL_ONLY"
            );

            loan.setStatus(LoanStatus.APPROVED);

            em.persist(loan);

            em.flush();

            loanId = loan.getLoanId();
            borrowerId = borrower.getBorrowerId();
            borrowerUserId = borrowerUser.getUserId();
            managerUserId = manager.getUserId();
        });
    }

    // Create an OTP record in a committed transaction.
    private void createPendingOtp(
            LocalDateTime expiry,
            int attempts
    ) {

        transactions.executeWithoutResult(status -> {

            Loan loan = em.find(Loan.class, loanId);

            Disbursement disbursement =
                    new Disbursement();

            disbursement.setLoan(loan);

            disbursement.setAmount(
                    new BigDecimal("1000.00")
            );

            disbursement.setOtpHash(
                    passwordEncoder.encode(TEST_OTP)
            );

            disbursement.setOtpExpiresAt(expiry);
            disbursement.setOtpAttempts(attempts);

            disbursement.setStatus(
                    DisbursementStatus.OTP_PENDING
            );

            em.persist(disbursement);
            em.flush();

            disbursementId =
                    disbursement.getDisbursementId();
        });
    }

    private OtpVerifyRequest createRequest(
            String otp
    ) {

        OtpVerifyRequest request =
                new OtpVerifyRequest();

        request.setOtp(otp);

        return request;
    }

    private Disbursement readDisbursement() {

        return transactions.execute(status ->
                disbursementRepository
                        .findById(disbursementId)
                        .orElseThrow()
        );
    }

    private Loan readLoan() {

        return transactions.execute(status ->
                loanRepository.findById(loanId)
                        .orElseThrow()
        );
    }

    private ResponseStatusException confirmInvalidOtp() {

        return assertThrows(
                ResponseStatusException.class,
                () -> service.confirmDisbursement(
                        disbursementId,
                        managerEmail,
                        createRequest(WRONG_OTP)
                )
        );
    }

    // TEST 1
    // Failed OTP attempt must persist in MySQL.
    @Test
    void invalidOtpAttemptPersistsInDatabase() {

        createPendingOtp(
                LocalDateTime.now().plusMinutes(5),
                0
        );

        ResponseStatusException exception =
                confirmInvalidOtp();

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        Disbursement updated = readDisbursement();

        assertEquals(
                1,
                updated.getOtpAttempts()
        );

        assertEquals(
                DisbursementStatus.OTP_PENDING,
                updated.getStatus()
        );

        assertEquals(
                LoanStatus.APPROVED,
                readLoan().getStatus()
        );
    }

    // TEST 2
    // The fifth failed attempt must invalidate the OTP.
    @Test
    void fifthInvalidAttemptPersistsExpiredStatus() {

        createPendingOtp(
                LocalDateTime.now().plusMinutes(5),
                4
        );

        ResponseStatusException exception =
                confirmInvalidOtp();

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS,
                exception.getStatusCode()
        );

        Disbursement updated = readDisbursement();

        assertEquals(
                5,
                updated.getOtpAttempts()
        );

        assertEquals(
                DisbursementStatus.EXPIRED,
                updated.getStatus()
        );

        assertEquals(
                LoanStatus.APPROVED,
                readLoan().getStatus()
        );
    }

    // TEST 3
    // Expired OTP status must remain saved.
    @Test
    void expiredOtpStatusPersistsInDatabase() {

        createPendingOtp(
                LocalDateTime.now().minusMinutes(1),
                0
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.confirmDisbursement(
                                disbursementId,
                                managerEmail,
                                createRequest(TEST_OTP)
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                DisbursementStatus.EXPIRED,
                readDisbursement().getStatus()
        );

        assertEquals(
                LoanStatus.APPROVED,
                readLoan().getStatus()
        );
    }

    // TEST 4
    // New OTP cannot be requested before expiration.
    @Test
    void repeatedOtpRequestIsBlocked() {

        createPendingOtp(
                LocalDateTime.now().plusMinutes(5),
                2
        );

        String originalHash =
                readDisbursement().getOtpHash();

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.requestOtp(loanId)
                );

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS,
                exception.getStatusCode()
        );

        Disbursement updated = readDisbursement();

        assertEquals(
                originalHash,
                updated.getOtpHash()
        );

        assertEquals(
                2,
                updated.getOtpAttempts()
        );

        verifyNoInteractions(otpDeliveryService);
    }

    // TEST 5
    // SMS failure after commit must preserve the OTP record.
    @Test
    void deliveryFailurePreservesCommittedOtp() {

        doThrow(
                new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "OTP delivery failed"
                )
        ).when(otpDeliveryService)
                .sendOtp(anyString(), anyString());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.requestOtp(loanId)
                );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode()
        );

        Disbursement saved = transactions.execute(status ->
                disbursementRepository
                        .findByLoanLoanId(loanId)
                        .orElseThrow()
        );

        assertNotNull(saved);

        assertEquals(
                DisbursementStatus.OTP_PENDING,
                saved.getStatus()
        );

        assertEquals(
                0,
                saved.getOtpAttempts()
        );

        assertNotNull(saved.getOtpHash());

        assertTrue(
                saved.getOtpExpiresAt()
                        .isAfter(LocalDateTime.now())
        );

        assertEquals(
                LoanStatus.APPROVED,
                readLoan().getStatus()
        );

        // Immediate retries must remain blocked.
        ResponseStatusException retryException =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.requestOtp(loanId)
                );

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS,
                retryException.getStatusCode()
        );

        verify(
                otpDeliveryService,
                times(1)
        ).sendOtp(anyString(), anyString());
    }

    // TEST 6
    // Two concurrent requests must not disburse twice.
    @Test
    void concurrentConfirmationSucceedsOnlyOnce()
            throws Exception {

        createPendingOtp(
                LocalDateTime.now().plusMinutes(5),
                0
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch start =
                new CountDownLatch(1);

        Callable<Integer> confirmation = () -> {

            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Concurrent test start timed out"
                );
            }

            try {

                service.confirmDisbursement(
                        disbursementId,
                        managerEmail,
                        createRequest(TEST_OTP)
                );

                return 200;

            } catch (ResponseStatusException exception) {

                return exception
                        .getStatusCode()
                        .value();
            }
        };

        try {

            Future<Integer> first =
                    executor.submit(confirmation);

            Future<Integer> second =
                    executor.submit(confirmation);

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS)
            );

            start.countDown();

            int firstStatus =
                    first.get(30, TimeUnit.SECONDS);

            int secondStatus =
                    second.get(30, TimeUnit.SECONDS);

            assertTrue(
                    (firstStatus == 200
                            && secondStatus == 409)
                            ||
                            (firstStatus == 409
                                    && secondStatus == 200),
                    "Exactly one confirmation must succeed"
            );

            assertEquals(
                    DisbursementStatus.COMPLETED,
                    readDisbursement().getStatus()
            );

            Loan updatedLoan = readLoan();

            assertEquals(
                    LoanStatus.ACTIVE,
                    updatedLoan.getStatus()
            );

            assertEquals(
                    0,
                    new BigDecimal("1000.00")
                            .compareTo(
                                    updatedLoan
                                            .getOutstandingBalance()
                            )
            );

            verify(
                    installmentScheduleService,
                    times(1)
            ).createMonthlySchedule(
                    any(Loan.class),
                    any(LocalDate.class)
            );

        } finally {

            start.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            30,
                            TimeUnit.SECONDS
                    ),
                    "Concurrent workers did not terminate"
            );
        }
    }

    // TEST 7
    // Database rollback must prevent SMS delivery.
    @Test
    void databaseRollbackPreventsSmsDelivery() {

        transactions.executeWithoutResult(status -> {

            service.requestOtp(loanId);

            // SMS must not be sent before commit.
            verifyNoInteractions(otpDeliveryService);

            status.setRollbackOnly();
        });

        boolean exists = transactions.execute(status ->
                disbursementRepository
                        .findByLoanLoanId(loanId)
                        .isPresent()
        );

        assertFalse(
                exists,
                "OTP must not exist after database rollback"
        );

        verifyNoInteractions(otpDeliveryService);

        assertEquals(
                LoanStatus.APPROVED,
                readLoan().getStatus()
        );
    }

    // Remove only records created by this test.
    @AfterEach
    void cleanUp() {

        if (loanId == null) {
            return;
        }

        transactions.executeWithoutResult(status -> {

            String database = (String) em
                    .createNativeQuery("SELECT DATABASE()")
                    .getSingleResult();

            assertEquals(
                    "quickcash_test_db",
                    database
            );

            em.createQuery(
                    "DELETE FROM Disbursement d " +
                            "WHERE d.loan.loanId = :loanId"
            ).setParameter(
                    "loanId",
                    loanId
            ).executeUpdate();

            em.flush();

            Loan loan = em.find(
                    Loan.class,
                    loanId
            );

            if (loan != null) {
                em.remove(loan);
            }

            em.flush();

            Borrower borrower = em.find(
                    Borrower.class,
                    borrowerId
            );

            if (borrower != null) {
                em.remove(borrower);
            }

            em.flush();

            User borrowerUser = em.find(
                    User.class,
                    borrowerUserId
            );

            if (borrowerUser != null) {
                em.remove(borrowerUser);
            }

            User manager = em.find(
                    User.class,
                    managerUserId
            );

            if (manager != null) {
                em.remove(manager);
            }

            em.flush();
        });
    }
}