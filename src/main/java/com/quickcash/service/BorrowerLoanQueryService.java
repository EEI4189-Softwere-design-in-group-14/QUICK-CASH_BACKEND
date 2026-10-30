
package com.quickcash.service;

import com.quickcash.dto.BorrowerLoanHistoryResponse;
import com.quickcash.dto.BorrowerLoanDetailsResponse;
import com.quickcash.dto.InstallmentResponse;

import com.quickcash.entity.Loan;
import com.quickcash.repository.LoanRepository;
import com.quickcash.repository.LoanInstallmentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class BorrowerLoanQueryService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;

    public BorrowerLoanQueryService(
            LoanRepository loanRepository,
            LoanInstallmentRepository installmentRepository
    ) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
    }

    // Method 1: Get all loans belonging to the borrower
    @Transactional(readOnly = true)
    public List<BorrowerLoanHistoryResponse> getMyLoans(
            String borrowerEmail
    ) {

        return loanRepository
                .findByBorrower_User_EmailIgnoreCaseOrderByLoanIdDesc(
                        borrowerEmail
                )
                .stream()
                .map(BorrowerLoanHistoryResponse::from)
                .toList();
    }

    // Method 2: Get details of a specific loan
    @Transactional(readOnly = true)
    public BorrowerLoanDetailsResponse getMyLoanDetails(
            Long loanId,
            String borrowerEmail
    ) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Loan not found"
                        )
                );

        String ownerEmail = loan.getBorrower()
                .getUser()
                .getEmail();

        if (!ownerEmail.equalsIgnoreCase(borrowerEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Loan not found"
            );
        }

        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Colombo")
        );

        List<InstallmentResponse> installments =
                installmentRepository
                        .findByLoanLoanIdOrderByInstallmentNumberAsc(
                                loanId
                        )
                        .stream()
                        .map(i -> new InstallmentResponse(i, today))
                        .toList();

        String interestRate = loan.getInterestRate() == null
                ? null
                : loan.getInterestRate().toString();

        String reviewedAt = loan.getReviewedAt() == null
                ? null
                : loan.getReviewedAt().toString();

        return new BorrowerLoanDetailsResponse(
                BorrowerLoanHistoryResponse.from(loan),
                loan.getRepaymentMonths(),
                interestRate,
                reviewedAt,
                installments
        );
    }
}
