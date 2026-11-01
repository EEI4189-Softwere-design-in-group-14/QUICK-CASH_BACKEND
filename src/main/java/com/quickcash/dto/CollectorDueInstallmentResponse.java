
package com.quickcash.dto;

import com.quickcash.entity.LoanInstallment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CollectorDueInstallmentResponse(
        Long installmentId,
        Long loanId,
        String loanCode,
        String borrowerName,
        String borrowerPhone,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal principalDue,
        BigDecimal interestDue,
        BigDecimal totalDue,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String dueStatus
) {

    public static CollectorDueInstallmentResponse from(
            LoanInstallment installment,
            LocalDate asOfDate
    ) {

        // Step 1: Get paid amount
        BigDecimal paid =
                installment.getPaidAmount() == null
                        ? BigDecimal.ZERO
                        : installment.getPaidAmount();

        // Step 2: Get interest amount
        BigDecimal interest =
                installment.getInterestDue() == null
                        ? BigDecimal.ZERO
                        : installment.getInterestDue();

        // Step 3: Calculate total installment
        BigDecimal totalDue = installment.getTotalDue();

        // Step 4: Calculate remaining amount
        BigDecimal remaining =
                totalDue.subtract(paid)
                        .max(BigDecimal.ZERO);

        // Step 5: Check installment status
        String dueStatus =
                installment.getDueDate().isBefore(asOfDate)
                        ? "OVERDUE"
                        : "DUE_ON_DATE";

        // Step 6: Return response
        return new CollectorDueInstallmentResponse(
                installment.getInstallmentId(),
                installment.getLoan().getLoanId(),
                installment.getLoan().getLoanCode(),
                installment.getLoan().getBorrower()
                        .getUser().getFullName(),
                installment.getLoan().getBorrower()
                        .getUser().getPhone(),
                installment.getInstallmentNumber(),
                installment.getDueDate(),
                installment.getPrincipalDue(),
                interest,
                totalDue,
                paid,
                remaining,
                dueStatus
        );
    }
}
