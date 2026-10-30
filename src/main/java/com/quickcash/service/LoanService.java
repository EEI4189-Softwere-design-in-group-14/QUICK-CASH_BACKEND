package com.quickcash.service;

import com.quickcash.dto.LoanApplicationRequest;
import com.quickcash.dto.LoanResponse;
import com.quickcash.entity.*;
import com.quickcash.repository.BorrowerRepository;
import com.quickcash.repository.GuarantorRepository;
import com.quickcash.repository.LoanRepository;
import com.quickcash.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.quickcash.dto.LoanApprovalRequest;
import com.quickcash.dto.LoanRejectionRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final GuarantorRepository guarantorRepository;
    private final BorrowerRepository borrowerRepository;
    private final UserRepository userRepository;

    public LoanService(
            LoanRepository loanRepository,
            GuarantorRepository guarantorRepository,
            BorrowerRepository borrowerRepository,
            UserRepository userRepository
    ) {
        this.loanRepository = loanRepository;
        this.guarantorRepository = guarantorRepository;
        this.borrowerRepository = borrowerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LoanResponse applyForLoan(
            String email,
            LoanApplicationRequest request
    ) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        Borrower borrower =
                borrowerRepository
                        .findByUserUserId(
                                user.getUserId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Borrower profile not found"
                                )
                        );

        if (borrower.getStatus()
                != BorrowerStatus.APPROVED) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Borrower must be approved before applying for a loan"
            );
        }

        /*
         * Prevent more than one open/unsettled loan.
         *
         * PENDING  = already submitted
         * APPROVED = approved but not yet disbursed
         * ACTIVE   = cash disbursed and repayment ongoing
         */
        Set<LoanStatus> openStatuses =
                Set.of(
                        LoanStatus.PENDING,
                        LoanStatus.APPROVED,
                        LoanStatus.ACTIVE
                );

        boolean existingLoan =
                loanRepository
                        .existsByBorrowerBorrowerIdAndStatusIn(
                                borrower.getBorrowerId(),
                                openStatuses
                        );

        if (existingLoan) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Borrower already has an open loan or loan application"
            );
        }

        Loan loan = new Loan();

        loan.setLoanCode(
                "LN-" +
                        UUID.randomUUID()
                                .toString()
                                .substring(0, 8)
                                .toUpperCase()
        );

        loan.setBorrower(borrower);

        loan.setRequestedAmount(
                request.getRequestedAmount()
        );

        loan.setPurpose(
                request.getPurpose().trim()
        );

        loan.setRepaymentMonths(
                request.getRepaymentMonths()
        );

        loan.setStatus(
                LoanStatus.PENDING
        );

        Loan savedLoan =
                loanRepository.save(loan);


        Guarantor guarantor =
                new Guarantor();

        guarantor.setLoan(savedLoan);

        guarantor.setFullName(
                request
                        .getGuarantorFullName()
                        .trim()
        );

        guarantor.setNic(
                request
                        .getGuarantorNic()
                        .trim()
                        .toUpperCase()
        );

        guarantor.setPhone(
                request
                        .getGuarantorPhone()
                        .trim()
        );

        guarantor.setAddress(
                request
                        .getGuarantorAddress()
                        .trim()
        );

        guarantor.setOccupation(
                request
                        .getGuarantorOccupation()
        );

        Guarantor savedGuarantor =
                guarantorRepository
                        .save(guarantor);

        return new LoanResponse(
                savedLoan,
                savedGuarantor
        );
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getMyLoans(
            String email
    ) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        Borrower borrower =
                borrowerRepository
                        .findByUserUserId(
                                user.getUserId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Borrower profile not found"
                                )
                        );

        return loanRepository
                .findByBorrowerBorrowerId(
                        borrower.getBorrowerId()
                )
                .stream()
                .map(loan -> {

                    Guarantor guarantor =
                            guarantorRepository
                                    .findByLoanLoanId(
                                            loan.getLoanId()
                                    )
                                    .orElse(null);

                    return new LoanResponse(
                            loan,
                            guarantor
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getPendingLoans() {

        return loanRepository
                .findByStatus(LoanStatus.PENDING)
                .stream()
                .map(loan -> {

                    Guarantor guarantor =
                            guarantorRepository
                                    .findByLoanLoanId(
                                            loan.getLoanId()
                                    )
                                    .orElse(null);

                    return new LoanResponse(
                            loan,
                            guarantor
                    );
                })
                .toList();
    }

    @Transactional
    public LoanResponse approveLoan(
            Long loanId,
            String managerEmail,
            LoanApprovalRequest request
    ) {

        Loan loan = loanRepository
                .findById(loanId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Loan not found"
                        )
                );

        if (loan.getStatus() != LoanStatus.PENDING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending loans can be approved"
            );
        }

        User manager = userRepository
                .findByEmail(managerEmail)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Manager not found"
                        )
                );

        BigDecimal approvedAmount =
                request.getApprovedAmount();

        /*
         * Do not approve more than the borrower requested.
         */
        if (approvedAmount.compareTo(
                loan.getRequestedAmount()
        ) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Approved amount cannot exceed requested amount"
            );
        }

        loan.setApprovedAmount(
                approvedAmount
        );

        loan.setInterestRate(
                request.getInterestRate()
        );

        loan.setStatus(
                LoanStatus.APPROVED
        );

        loan.setReviewedAt(
                LocalDateTime.now()
        );

        loan.setReviewedBy(manager);

        loan.setRejectionReason(null);

        Loan savedLoan =
                loanRepository.save(loan);

        Guarantor guarantor =
                guarantorRepository
                        .findByLoanLoanId(
                                savedLoan.getLoanId()
                        )
                        .orElse(null);

        return new LoanResponse(
                savedLoan,
                guarantor
        );
    }

    @Transactional
    public LoanResponse rejectLoan(
            Long loanId,
            String managerEmail,
            LoanRejectionRequest request
    ) {

        Loan loan = loanRepository
                .findById(loanId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Loan not found"
                        )
                );

        if (loan.getStatus() != LoanStatus.PENDING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending loans can be rejected"
            );
        }

        User manager = userRepository
                .findByEmail(managerEmail)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Manager not found"
                        )
                );

        loan.setStatus(
                LoanStatus.REJECTED
        );

        loan.setReviewedAt(
                LocalDateTime.now()
        );

        loan.setReviewedBy(manager);

        loan.setRejectionReason(
                request.getReason().trim()
        );

        Loan savedLoan =
                loanRepository.save(loan);

        Guarantor guarantor =
                guarantorRepository
                        .findByLoanLoanId(
                                savedLoan.getLoanId()
                        )
                        .orElse(null);

        return new LoanResponse(
                savedLoan,
                guarantor
        );
    }
}