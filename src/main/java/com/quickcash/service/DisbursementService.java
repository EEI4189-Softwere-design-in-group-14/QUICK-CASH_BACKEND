package com.quickcash.service;

import com.quickcash.dto.DisbursementResponse;
import com.quickcash.dto.OtpRequestResponse;
import com.quickcash.dto.OtpVerifyRequest;

import com.quickcash.entity.AuditAction;
import com.quickcash.entity.Disbursement;
import com.quickcash.entity.DisbursementStatus;
import com.quickcash.entity.Loan;
import com.quickcash.entity.LoanStatus;
import com.quickcash.entity.User;

import com.quickcash.repository.DisbursementRepository;
import com.quickcash.repository.LoanRepository;
import com.quickcash.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Locale;

@Service
public class DisbursementService {

    private final DisbursementRepository disbursementRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final InstallmentScheduleService installmentScheduleService;
    private final OtpDeliveryService otpDeliveryService;

    private final SecureRandom secureRandom = new SecureRandom();

    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int OTP_EXPIRATION_MINUTES = 5;

    public DisbursementService(
            DisbursementRepository disbursementRepository,
            LoanRepository loanRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            InstallmentScheduleService installmentScheduleService,
            OtpDeliveryService otpDeliveryService
    ) {
        this.disbursementRepository = disbursementRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.installmentScheduleService = installmentScheduleService;
        this.otpDeliveryService = otpDeliveryService;
    }

    // Generate OTP for an approved loan.
    @Transactional
    public OtpRequestResponse requestOtp(Long loanId) {

        // Lock the loan to prevent concurrent requests.
        Loan loan = loanRepository.findByIdForUpdate(loanId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Loan not found"
                        )
                );

        // Only approved loans can be disbursed.
        if (loan.getStatus() != LoanStatus.APPROVED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only approved loans can be disbursed"
            );
        }

        if (loan.getApprovedAmount() == null) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Approved amount is missing"
            );
        }

        // Lock the existing disbursement record.
        Disbursement disbursement = disbursementRepository
                .findByLoanIdForUpdate(loanId)
                .orElseGet(Disbursement::new);

        // Prevent duplicate disbursements.
        if (disbursement.getStatus()
                == DisbursementStatus.COMPLETED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This loan has already been disbursed"
            );
        }

        // Prevent repeated OTP requests before expiration.
        if (disbursement.getOtpExpiresAt() != null
                && LocalDateTime.now().isBefore(
                disbursement.getOtpExpiresAt())) {

            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait until the previous OTP expires"
            );
        }

        // Obtain the registered borrower's phone number.
        String phoneNumber = loan.getBorrower()
                .getUser()
                .getPhone();

        if (phoneNumber == null || phoneNumber.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Borrower phone number is missing"
            );
        }

        // Generate a secure six-digit OTP.
        String otp = String.format(
                Locale.ROOT,
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        LocalDateTime expiry = LocalDateTime.now()
                .plusMinutes(OTP_EXPIRATION_MINUTES);

        disbursement.setLoan(loan);

        disbursement.setAmount(
                loan.getApprovedAmount()
        );

        // Store only the hashed OTP.
        disbursement.setOtpHash(
                passwordEncoder.encode(otp)
        );

        // Reset failed attempts for a new OTP.
        disbursement.setOtpAttempts(0);

        disbursement.setOtpExpiresAt(expiry);
        disbursement.setOtpVerifiedAt(null);
        disbursement.setDisbursedAt(null);
        disbursement.setConfirmedBy(null);

        disbursement.setStatus(
                DisbursementStatus.OTP_PENDING
        );

        Disbursement saved =
                disbursementRepository.save(disbursement);

        // Submit SMS only after a successful database commit.
        // If the transaction rolls back, no SMS will be sent.
        // If SMS delivery fails after commit, retain the OTP
        // record and cooldown to prevent immediate retries.

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        otpDeliveryService.sendOtp(
                                phoneNumber,
                                otp
                        );
                    }
                }
        );

        return new OtpRequestResponse(
                saved.getDisbursementId(),
                "OTP submitted to SMS gateway",
                expiry
        );
    }

    // Confirm disbursement using a valid OTP.
    @Transactional(noRollbackFor = {
            OtpExpiredException.class,
            OtpAttemptException.class
    })
    public DisbursementResponse confirmDisbursement(
            Long disbursementId,
            String managerEmail,
            OtpVerifyRequest request
    ) {

        // Find the associated loan ID.
        Long loanId = disbursementRepository
                .findLoanIdByDisbursementId(disbursementId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Disbursement not found"
                        )
                );

        // Always lock the loan first.
        Loan loan = loanRepository
                .findByIdForUpdate(loanId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Loan not found"
                        )
                );

        // Lock the disbursement record second.
        Disbursement disbursement = disbursementRepository
                .findByIdForUpdate(disbursementId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Disbursement not found"
                        )
                );

        // Reject further attempts after the attempt limit.
        if (disbursement.getStatus()
                == DisbursementStatus.EXPIRED
                && disbursement.getOtpAttempts()
                >= MAX_OTP_ATTEMPTS) {

            throw new OtpAttemptException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Maximum OTP attempts exceeded"
            );
        }

        // Prevent repeated confirmation.
        if (disbursement.getStatus()
                != DisbursementStatus.OTP_PENDING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Disbursement is not awaiting OTP verification"
            );
        }

        // Only approved loans can be disbursed.
        if (loan.getStatus() != LoanStatus.APPROVED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only approved loans can be disbursed"
            );
        }

        // Reject expired OTPs.
        if (disbursement.getOtpExpiresAt() == null
                || !LocalDateTime.now().isBefore(
                disbursement.getOtpExpiresAt())) {

            disbursement.setStatus(
                    DisbursementStatus.EXPIRED
            );

            disbursementRepository.save(disbursement);

            throw new OtpExpiredException();
        }

        // Check the maximum failed attempt limit.
        if (disbursement.getOtpAttempts()
                >= MAX_OTP_ATTEMPTS) {

            disbursement.setStatus(
                    DisbursementStatus.EXPIRED
            );

            disbursementRepository.save(disbursement);

            throw new OtpAttemptException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Maximum OTP attempts exceeded"
            );
        }

        // Validate OTP format.
        if (request == null
                || request.getOtp() == null
                || !request.getOtp().matches("[0-9]{6}")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OTP must contain exactly 6 digits"
            );
        }

        // Verify the submitted OTP against the stored hash.
        boolean validOtp = passwordEncoder.matches(
                request.getOtp(),
                disbursement.getOtpHash()
        );

        // Handle invalid OTP attempts.
        if (!validOtp) {

            int attempts =
                    disbursement.getOtpAttempts() + 1;

            disbursement.setOtpAttempts(attempts);

            // Invalidate OTP after five failed attempts.
            if (attempts >= MAX_OTP_ATTEMPTS) {

                disbursement.setStatus(
                        DisbursementStatus.EXPIRED
                );

                disbursementRepository.save(disbursement);

                throw new OtpAttemptException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Maximum OTP attempts exceeded. "
                                + "Wait before requesting a new OTP."
                );
            }

            // Persist the failed attempt count.
            disbursementRepository.save(disbursement);

            throw new OtpAttemptException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid OTP"
            );
        }

        // Find the authenticated manager or owner.
        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Manager not found"
                        )
                );

        LocalDateTime now = LocalDateTime.now();

        String planType = loan.getRepaymentPlanType();

        // Step 1: Set the correct outstanding balance.
        if ("DAILY_FIXED_TOTAL".equals(planType)) {

            if (loan.getApprovedAmount() == null
                    || loan.getTotalRepaymentAmount() == null
                    || loan.getInterestAmount() == null
                    || loan.getRepaymentDays() == null
                    || loan.getRepaymentDays() <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Daily repayment details are incomplete"
                );
            }

            BigDecimal expectedTotal =
                    loan.getApprovedAmount()
                            .add(loan.getInterestAmount());

            if (expectedTotal.compareTo(
                    loan.getTotalRepaymentAmount()) != 0) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Loan repayment amounts are inconsistent"
                );
            }

            // Daily loan includes principal and interest.
            loan.setOutstandingBalance(
                    loan.getTotalRepaymentAmount()
            );

        } else if ("MONTHLY_PRINCIPAL_ONLY".equals(planType)) {

            // Preserve existing monthly loan behaviour.
            loan.setOutstandingBalance(
                    loan.getApprovedAmount()
            );

        } else {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Unsupported repayment plan type"
            );
        }

        // Step 2: Complete the disbursement.
        disbursement.setOtpVerifiedAt(now);
        disbursement.setDisbursedAt(now);
        disbursement.setConfirmedBy(manager);

        disbursement.setStatus(
                DisbursementStatus.COMPLETED
        );

        // Step 3: Activate the loan.
        loan.setStatus(LoanStatus.ACTIVE);

        loanRepository.save(loan);

        // Step 4: Save the completed disbursement.
        Disbursement saved =
                disbursementRepository.save(disbursement);

        // Step 5: Generate the correct installment schedule.
        if ("DAILY_FIXED_TOTAL".equals(planType)) {

            installmentScheduleService.createDailySchedule(
                    loan,
                    saved.getDisbursedAt().toLocalDate()
            );

        } else {

            installmentScheduleService.createMonthlySchedule(
                    loan,
                    saved.getDisbursedAt().toLocalDate()
            );
        }

        // Step 6: Record the successful disbursement.
        auditService.record(
                manager,
                AuditAction.DISBURSEMENT_COMPLETED,
                "DISBURSEMENT",
                saved.getDisbursementId(),
                loan.getLoanCode(),
                "Loan cash disbursement completed for LKR "
                        + saved.getAmount()
        );

        return new DisbursementResponse(saved);
    }

    // Preserve expired status when returning HTTP 400.
    private static class OtpExpiredException
            extends ResponseStatusException {

        public OtpExpiredException() {

            super(
                    HttpStatus.BAD_REQUEST,
                    "OTP has expired"
            );
        }
    }

    // Preserve failed attempt count when rejecting an OTP.
    private static class OtpAttemptException
            extends ResponseStatusException {

        public OtpAttemptException(
                HttpStatus status,
                String message
        ) {
            super(status, message);
        }
    }
}