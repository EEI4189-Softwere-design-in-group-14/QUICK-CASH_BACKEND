package com.quickcash.service;

import com.quickcash.dto.BorrowerResponse;
import com.quickcash.dto.LoanResponse;
import com.quickcash.dto.PaymentResponse;

import com.quickcash.entity.Borrower;
import com.quickcash.entity.Guarantor;
import com.quickcash.entity.Loan;
import com.quickcash.entity.LoanStatus;
import com.quickcash.entity.Payment;

import com.quickcash.repository.BorrowerRepository;
import com.quickcash.repository.GuarantorRepository;
import com.quickcash.repository.LoanRepository;
import com.quickcash.repository.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ManagerReportService {

    private final LoanRepository loanRepository;
    private final GuarantorRepository guarantorRepository;
    private final PaymentRepository paymentRepository;
    private final BorrowerRepository borrowerRepository;

    public ManagerReportService(
            LoanRepository loanRepository,
            GuarantorRepository guarantorRepository,
            PaymentRepository paymentRepository,
            BorrowerRepository borrowerRepository
    ) {
        this.loanRepository = loanRepository;
        this.guarantorRepository = guarantorRepository;
        this.paymentRepository = paymentRepository;
        this.borrowerRepository = borrowerRepository;
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getActiveLoans() {

        return loanRepository
                .findByStatus(LoanStatus.ACTIVE)
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
    public List<PaymentResponse> getAllPayments() {

        return paymentRepository
                .findAll()
                .stream()
                .sorted(
                        (p1, p2) ->
                                p2.getPaymentDate()
                                        .compareTo(
                                                p1.getPaymentDate()
                                        )
                )
                .map(PaymentResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BorrowerResponse> getAllBorrowers() {

        return borrowerRepository
                .findAll()
                .stream()
                .map(BorrowerResponse::new)
                .toList();
    }
}