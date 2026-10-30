
package com.quickcash.dto;

import com.quickcash.entity.LoanInstallment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentResponse(

        Long installmentId,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal principalDue,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String status

) {

    public InstallmentResponse(
            LoanInstallment installment,
            LocalDate today
    ) {

        this(
                installment.getInstallmentId(),
                installment.getInstallmentNumber(),
                installment.getDueDate(),
                installment.getPrincipalDue(),
                getPaid(installment),
                getRemaining(installment),
                calculateStatus(installment, today)
        );
    }

    private static BigDecimal getPaid(
            LoanInstallment installment
    ) {
        return installment.getPaidAmount() == null
                ? BigDecimal.ZERO
                : installment.getPaidAmount();
    }

    private static BigDecimal getRemaining(
            LoanInstallment installment
    ) {
        return installment.getPrincipalDue()
                .subtract(getPaid(installment))
                .max(BigDecimal.ZERO);
    }

    private static String calculateStatus(
            LoanInstallment installment,
            LocalDate today
    ) {

        BigDecimal paid = getPaid(installment);
        BigDecimal due = installment.getPrincipalDue();
        LocalDate dueDate = installment.getDueDate();

        if (paid.compareTo(due) >= 0) {
            return "PAID";
        }

        if (dueDate.isBefore(today)) {
            return "OVERDUE";
        }

        if (paid.compareTo(BigDecimal.ZERO) > 0) {
            return "PARTIALLY_PAID";
        }

        if (!dueDate.isAfter(today.plusDays(3))) {
            return "DUE_SOON";
        }

        return "UPCOMING";
    }
}
