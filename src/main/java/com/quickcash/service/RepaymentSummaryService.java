
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
import java.time.LocalDate;
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

        // Step 1: Find loan
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Loan not found"
                        )
                );

        // Step 2: Security - borrower can access only own loan
        String ownerEmail = loan.getBorrower()
                .getUser()
                .getEmail();

        if (borrowerEmail == null
                || !ownerEmail.equalsIgnoreCase(borrowerEmail)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Loan not found"
            );
        }

        // Step 3: Load loan installments
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

        // Step 4: Calculate total principal only
        // Do not include interest in totalPrincipal.
        BigDecimal totalPrincipal = installments.stream()
                .map(LoanInstallment::getPrincipalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Step 5: Calculate total amount already paid
        BigDecimal totalPaid = installments.stream()
                .map(this::paidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Step 6: Count all installments
        int totalInstallments = installments.size();

        // Step 7: Count fully paid installments
        // Principal + Interest must both be paid.
        int paidInstallments = (int) installments.stream()
                .filter(installment ->
                        paidAmount(installment).compareTo(
                                installment.getTotalDue()
                        ) >= 0
                )
                .count();

        // Step 8: Count remaining installments
        int remainingInstallments =
                totalInstallments - paidInstallments;

        // Step 9: Find the first unpaid installment
        LoanInstallment nextInstallment = installments.stream()
                .filter(installment ->
                        paidAmount(installment).compareTo(
                                installment.getTotalDue()
                        ) < 0
                )
                .findFirst()
                .orElse(null);

        Integer nextInstallmentNumber = null;
        LocalDate nextDueDate = null;

        BigDecimal nextInstallmentRemaining =
                BigDecimal.ZERO;

        // Step 10: Calculate next installment remaining
        if (nextInstallment != null) {

            nextInstallmentNumber =
                    nextInstallment.getInstallmentNumber();

            nextDueDate =
                    nextInstallment.getDueDate();

            // Total due = Principal + Interest
            BigDecimal totalDue =
                    nextInstallment.getTotalDue();

            BigDecimal alreadyPaid =
                    paidAmount(nextInstallment);

            // Remaining = Total due - Already paid
            nextInstallmentRemaining =
                    totalDue.subtract(alreadyPaid)
                            .max(BigDecimal.ZERO);
        }

        // Step 11: Return repayment summary
        // Outstanding balance comes from the existing loan.
        // No database balance is modified.
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

    // Helper: handle null paid amounts safely
    private BigDecimal paidAmount(
            LoanInstallment installment
    ) {
        return installment.getPaidAmount() == null
                ? BigDecimal.ZERO
                : installment.getPaidAmount();
    }
}
