
package com.quickcash.service;

import com.quickcash.dto.BorrowerPaymentHistoryResponse;
import com.quickcash.entity.Loan;
import com.quickcash.repository.LoanRepository;
import com.quickcash.repository.PaymentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BorrowerPaymentHistoryService {

    private final PaymentRepository paymentRepository;
    private final LoanRepository loanRepository;

    public BorrowerPaymentHistoryService(
            PaymentRepository paymentRepository,
            LoanRepository loanRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.loanRepository = loanRepository;
    }

    // All payments belonging to the logged-in borrower.
    @Transactional(readOnly = true)
    public List<BorrowerPaymentHistoryResponse> getMyPayments(
            String borrowerEmail
    ) {

        return paymentRepository
                .findByLoanBorrowerUserEmailOrderByPaymentDateDesc(
                        borrowerEmail
                )
                .stream()
                .map(BorrowerPaymentHistoryResponse::from)
                .toList();
    }

    // Payments for one loan, after verifying ownership.
    @Transactional(readOnly = true)
    public List<BorrowerPaymentHistoryResponse> getMyLoanPayments(
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

        String loanOwnerEmail = loan
                .getBorrower()
                .getUser()
                .getEmail();

        if (!loanOwnerEmail.equalsIgnoreCase(borrowerEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Loan not found"
            );
        }

        return paymentRepository
                .findByLoanLoanIdOrderByPaymentDateDesc(loanId)
                .stream()
                .map(BorrowerPaymentHistoryResponse::from)
                .toList();
    }
}
