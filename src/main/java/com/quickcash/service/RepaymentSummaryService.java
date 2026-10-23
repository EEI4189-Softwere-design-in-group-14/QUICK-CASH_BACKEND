
package com.quickcash.service;

import com.quickcash.dto.RepaymentSummaryResponse;
import com.quickcash.entity.Loan;
import com.quickcash.entity.LoanInstallment;
import com.quickcash.repository.LoanInstallmentRepository;
import com.quickcash.repository.LoanRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RepaymentSummaryService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;

    public RepaymentSummaryService(
            LoanRepository loanRepository,
            LoanInstallmentRepository installmentRepository
    ) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
    }

    @Transactional(readOnly = true)
    public RepaymentSummaryResponse getMyRepaymentSummary(
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

        // Borrowers can only access their own loans.
        String ownerEmail = loan.getBorrower()
                .getUser()
                .getEmail();

        if (!ownerEmail.equalsIgnoreCase(borrowerEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Loan not found"
            );
        }

        List<LoanInstallment> installments =
                installmentRepository
                        .findByLoanLoanIdOrderByInstallmentNumberAsc(
                                loanId
                        );

        if (installments.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Repayment schedule is not available for this loan"
            );
        }

        // Total scheduled principal.
        BigDecimal totalPrincipal = installments.stream()
                .map(LoanInstallment::getPrincipalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total allocated payments.
        BigDecimal totalPaid = installments.stream()
                .map(i -> paidAmount(i))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalInstallments = installments.size();

        int paidInstallments = (int) installments.stream()
                .filter(i ->
                        paidAmount(i).compareTo(
                                i.getPrincipalDue()
                        ) >= 0
                )
                .count();

        int remainingInstallments =
                totalInstallments - paidInstallments;

        // Find earliest installment that is not fully paid.
        LoanInstallment nextInstallment = installments.stream()
                .filter(i ->
                        paidAmount(i).compareTo(
                                i.getPrincipalDue()
                        ) < 0
                )
                .findFirst()
                .orElse(null);

        Integer nextInstallmentNumber = null;
        java.time.LocalDate nextDueDate = null;
        BigDecimal nextInstallmentRemaining = BigDecimal.ZERO;

        if (nextInstallment != null) {

            nextInstallmentNumber =
                    nextInstallment.getInstallmentNumber();

            nextDueDate = nextInstallment.getDueDate();

            nextInstallmentRemaining =
                    nextInstallment.getPrincipalDue()
                            .subtract(
                                    paidAmount(nextInstallment)
                            );
        }

        return new RepaymentSummaryResponse(
                loan.getLoanId(),
                loan.getLoanCode(),
                totalPrincipal,
                totalPaid,
                loan.getOutstandingBalance(),
                totalInstallments,
                paidInstallments,
                remainingInstallments,
                nextInstallmentNumber,
                nextDueDate,
                nextInstallmentRemaining
        );
    }

    private BigDecimal paidAmount(LoanInstallment installment) {
        return installment.getPaidAmount() == null
                ? BigDecimal.ZERO
                : installment.getPaidAmount();
    }
}
