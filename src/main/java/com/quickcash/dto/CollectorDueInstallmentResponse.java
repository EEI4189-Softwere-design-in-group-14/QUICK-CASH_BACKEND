
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
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String dueStatus
) {

    public static CollectorDueInstallmentResponse from(
            LoanInstallment installment,
            LocalDate asOfDate
    ) {

        BigDecimal paid = installment.getPaidAmount() == null
                ? BigDecimal.ZERO
                : installment.getPaidAmount();

        BigDecimal remaining = installment
                .getPrincipalDue()
                .subtract(paid)
                .max(BigDecimal.ZERO);

        String dueStatus = installment.getDueDate()
                .isBefore(asOfDate)
                ? "OVERDUE"
                : "DUE_ON_DATE";

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
                paid,
                remaining,
                dueStatus
        );
    }
}

