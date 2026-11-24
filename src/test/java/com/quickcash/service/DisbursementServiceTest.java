package com.quickcash.service;

import com.quickcash.dto.OtpVerifyRequest;
import com.quickcash.entity.*;
import com.quickcash.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DisbursementServiceTest {

    private DisbursementRepository disbursementRepository;
    private LoanRepository loanRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuditService auditService;
    private InstallmentScheduleService installmentScheduleService;
    private OtpDeliveryService otpDeliveryService;

    private DisbursementService service;

    private Loan loan;
    private Disbursement disbursement;
    private User manager;

    private static final Long LOAN_ID = 10L;
    private static final Long DISBURSEMENT_ID = 20L;

    private static final String MANAGER_EMAIL =
            "manager@test.com";

    @BeforeEach
    void setUp() {

        disbursementRepository =
                mock(DisbursementRepository.class);

        loanRepository =
                mock(LoanRepository.class);

        userRepository =
                mock(UserRepository.class);

        passwordEncoder =
                mock(PasswordEncoder.class);

        auditService =
                mock(AuditService.class);

        installmentScheduleService =
                mock(InstallmentScheduleService.class);

        otpDeliveryService =
                mock(OtpDeliveryService.class);

        service = new DisbursementService(
                disbursementRepository,
                loanRepository,
                userRepository,
                passwordEncoder,
                auditService,
                installmentScheduleService,
                otpDeliveryService
        );

        loan = new Loan();

        loan.setStatus(LoanStatus.APPROVED);

        loan.setApprovedAmount(
                new BigDecimal("10000.00")
        );

        loan.setRepaymentPlanType(
                "MONTHLY_PRINCIPAL_ONLY"
        );

        disbursement = new Disbursement();

        disbursement.setDisbursementId(
                DISBURSEMENT_ID
        );

        disbursement.setLoan(loan);

        disbursement.setAmount(
                new BigDecimal("10000.00")
        );

        disbursement.setOtpHash("test-otp-hash");

        disbursement.setOtpAttempts(0);

        disbursement.setOtpExpiresAt(
                LocalDateTime.now().plusMinutes(5)
        );

        disbursement.setStatus(
                DisbursementStatus.OTP_PENDING
        );

        manager = new User();

        when(disbursementRepository
                .findLoanIdByDisbursementId(DISBURSEMENT_ID))
                .thenReturn(Optional.of(LOAN_ID));

        when(loanRepository.findByIdForUpdate(LOAN_ID))
                .thenReturn(Optional.of(loan));

        when(disbursementRepository
                .findByIdForUpdate(DISBURSEMENT_ID))
                .thenReturn(Optional.of(disbursement));

        when(disbursementRepository.save(
                any(Disbursement.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        when(userRepository.findByEmail(MANAGER_EMAIL))
                .thenReturn(Optional.of(manager));
    }

    private OtpVerifyRequest createRequest(String otp) {

        OtpVerifyRequest request =
                new OtpVerifyRequest();

        request.setOtp(otp);

        return request;
    }

    // Test 1: Incorrect OTP increases attempt count.
    @Test
    void invalidOtpIncreasesAttemptCount() {

        when(passwordEncoder.matches(
                "111111", "test-otp-hash"))
                .thenReturn(false);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.confirmDisbursement(
                                DISBURSEMENT_ID,
                                MANAGER_EMAIL,
                                createRequest("111111")
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                1,
                disbursement.getOtpAttempts()
        );

        assertEquals(
                DisbursementStatus.OTP_PENDING,
                disbursement.getStatus()
        );

        verify(disbursementRepository)
                .save(disbursement);

        verifyNoInteractions(
                installmentScheduleService
        );
    }

    // Test 2: Five invalid attempts invalidate the OTP.
    @Test
    void fifthInvalidAttemptExpiresOtp() {

        disbursement.setOtpAttempts(4);

        when(passwordEncoder.matches(
                "111111", "test-otp-hash"))
                .thenReturn(false);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.confirmDisbursement(
                                DISBURSEMENT_ID,
                                MANAGER_EMAIL,
                                createRequest("111111")
                        )
                );

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS,
                exception.getStatusCode()
        );

        assertEquals(
                5,
                disbursement.getOtpAttempts()
        );

        assertEquals(
                DisbursementStatus.EXPIRED,
                disbursement.getStatus()
        );

        verify(disbursementRepository)
                .save(disbursement);

        verifyNoInteractions(
                installmentScheduleService
        );
    }

    // Test 3: Further attempts are blocked.
    @Test
    void furtherAttemptsAreBlocked() {

        disbursement.setOtpAttempts(5);

        disbursement.setStatus(
                DisbursementStatus.EXPIRED
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.confirmDisbursement(
                                DISBURSEMENT_ID,
                                MANAGER_EMAIL,
                                createRequest("123456")
                        )
                );

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS,
                exception.getStatusCode()
        );

        verifyNoInteractions(passwordEncoder);

        verifyNoInteractions(
                installmentScheduleService
        );
    }

    // Test 4: Expired OTP is rejected.
    @Test
    void expiredOtpIsRejected() {

        disbursement.setOtpExpiresAt(
                LocalDateTime.now().minusMinutes(1)
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.confirmDisbursement(
                                DISBURSEMENT_ID,
                                MANAGER_EMAIL,
                                createRequest("123456")
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                DisbursementStatus.EXPIRED,
                disbursement.getStatus()
        );

        verify(disbursementRepository)
                .save(disbursement);

        verifyNoInteractions(passwordEncoder);
    }

    // Test 5: Completed disbursement cannot be repeated.
    @Test
    void completedDisbursementCannotBeRepeated() {

        disbursement.setStatus(
                DisbursementStatus.COMPLETED
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.confirmDisbursement(
                                DISBURSEMENT_ID,
                                MANAGER_EMAIL,
                                createRequest("123456")
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verifyNoInteractions(passwordEncoder);

        verifyNoInteractions(
                installmentScheduleService
        );
    }

    // Test 6: Invalid OTP format is rejected.
    @Test
    void invalidOtpFormatIsRejected() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.confirmDisbursement(
                                DISBURSEMENT_ID,
                                MANAGER_EMAIL,
                                createRequest("123")
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(passwordEncoder);
    }

    // Test 7: Non-approved loan cannot be disbursed.
    @Test
    void nonApprovedLoanCannotBeDisbursed() {

        loan.setStatus(LoanStatus.ACTIVE);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.confirmDisbursement(
                                DISBURSEMENT_ID,
                                MANAGER_EMAIL,
                                createRequest("123456")
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verifyNoInteractions(passwordEncoder);

        verifyNoInteractions(
                installmentScheduleService
        );
    }

    // Test 8: Repeated OTP requests are blocked.
    @Test
    void repeatedOtpRequestIsBlocked() {

        when(disbursementRepository
                .findByLoanIdForUpdate(LOAN_ID))
                .thenReturn(Optional.of(disbursement));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.requestOtp(LOAN_ID)
                );

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS,
                exception.getStatusCode()
        );

        verifyNoInteractions(otpDeliveryService);
    }

    // Test 9: Correct OTP activates a monthly loan.
    @Test
    void validOtpActivatesMonthlyLoan() {

        when(passwordEncoder.matches(
                "123456", "test-otp-hash"))
                .thenReturn(true);

        service.confirmDisbursement(
                DISBURSEMENT_ID,
                MANAGER_EMAIL,
                createRequest("123456")
        );

        assertEquals(
                LoanStatus.ACTIVE,
                loan.getStatus()
        );

        assertEquals(
                DisbursementStatus.COMPLETED,
                disbursement.getStatus()
        );

        assertEquals(
                new BigDecimal("10000.00"),
                loan.getOutstandingBalance()
        );

        assertNotNull(
                disbursement.getOtpVerifiedAt()
        );

        assertNotNull(
                disbursement.getDisbursedAt()
        );

        verify(loanRepository).save(loan);

        verify(installmentScheduleService)
                .createMonthlySchedule(
                        eq(loan),
                        any(LocalDate.class)
                );
    }
}