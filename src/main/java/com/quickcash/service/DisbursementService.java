package com.quickcash.service;

import com.quickcash.dto.DisbursementResponse;
import com.quickcash.dto.OtpRequestResponse;
import com.quickcash.dto.OtpVerifyRequest;

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
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class DisbursementService {

    private final DisbursementRepository disbursementRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public DisbursementService(
            DisbursementRepository disbursementRepository,
            LoanRepository loanRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {

        this.disbursementRepository =
                disbursementRepository;

        this.loanRepository =
                loanRepository;

        this.userRepository =
                userRepository;

        this.passwordEncoder =
                passwordEncoder;
    }

    @Transactional
    public OtpRequestResponse requestOtp(
            Long loanId
    ) {

        Loan loan = loanRepository
                .findById(loanId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Loan not found"
                        )
                );

        if (loan.getStatus()
                != LoanStatus.APPROVED) {

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

        Disbursement disbursement =
                disbursementRepository
                        .findByLoanLoanId(loanId)
                        .orElseGet(Disbursement::new);

        if (disbursement.getStatus()
                == DisbursementStatus.COMPLETED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This loan has already been disbursed"
            );
        }

        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        LocalDateTime expiry =
                LocalDateTime.now()
                        .plusMinutes(5);

        disbursement.setLoan(loan);

        disbursement.setAmount(
                loan.getApprovedAmount()
        );

        disbursement.setOtpHash(
                passwordEncoder.encode(otp)
        );

        disbursement.setOtpExpiresAt(expiry);

        disbursement.setOtpVerifiedAt(null);
        disbursement.setDisbursedAt(null);
        disbursement.setConfirmedBy(null);

        disbursement.setStatus(
                DisbursementStatus.OTP_PENDING
        );

        Disbursement saved =
                disbursementRepository
                        .save(disbursement);

        /*
         * DEVELOPMENT ONLY.
         *
         * Later replace this with an SMS provider.
         */
        System.out.println();
        System.out.println(
                "======================================"
        );

        System.out.println(
                "QUICKCASH DEVELOPMENT OTP: " + otp
        );

        System.out.println(
                "LOAN: " + loan.getLoanCode()
        );

        System.out.println(
                "OTP EXPIRES: " + expiry
        );

        System.out.println(
                "======================================"
        );
        System.out.println();

        return new OtpRequestResponse(
                saved.getDisbursementId(),
                "OTP generated successfully",
                expiry
        );
    }

    @Transactional
    public DisbursementResponse confirmDisbursement(
            Long disbursementId,
            String managerEmail,
            OtpVerifyRequest request
    ) {

        Disbursement disbursement =
                disbursementRepository
                        .findById(disbursementId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Disbursement not found"
                                )
                        );

        if (disbursement.getStatus()
                != DisbursementStatus.OTP_PENDING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Disbursement is not awaiting OTP verification"
            );
        }

        if (LocalDateTime.now()
                .isAfter(
                        disbursement.getOtpExpiresAt()
                )) {

            disbursement.setStatus(
                    DisbursementStatus.EXPIRED
            );

            disbursementRepository
                    .save(disbursement);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OTP has expired"
            );
        }

        boolean validOtp =
                passwordEncoder.matches(
                        request.getOtp(),
                        disbursement.getOtpHash()
                );

        if (!validOtp) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid OTP"
            );
        }

        User manager =
                userRepository
                        .findByEmail(managerEmail)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Manager not found"
                                )
                        );

        LocalDateTime now =
                LocalDateTime.now();

        disbursement.setOtpVerifiedAt(now);

        disbursement.setDisbursedAt(now);

        disbursement.setConfirmedBy(manager);

        disbursement.setStatus(
                DisbursementStatus.COMPLETED
        );

        Loan loan =
                disbursement.getLoan();

        loan.setStatus(
                LoanStatus.ACTIVE
        );

        /*
         * Until the final interest/repayment formula
         * is confirmed, use the approved principal
         * as the initial outstanding balance.
         */
        loan.setOutstandingBalance(
                loan.getApprovedAmount()
        );

        loanRepository.save(loan);

        Disbursement saved =
                disbursementRepository
                        .save(disbursement);

        return new DisbursementResponse(saved);
    }
}