
package com.quickcash.dto;

import com.quickcash.entity.Loan;
import com.quickcash.entity.LoanInstallment;
import com.quickcash.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record ManagerOverdueInstallmentResponse(
        Long installmentId,
        Long loanId,
        String loanCode,
        String borrowerName,
        String borrowerPhone,
        Long collectorId,
        String collectorName,
        Integer installmentNumber,
        LocalDate dueDate,
        long daysOverdue,
        String agingBucket,
        BigDecimal principalDue,
        BigDecimal paidAmount,
        BigDecimal remainingAmount
) {

    public static ManagerOverdueInstallmentResponse from(
            LoanInstallment installment,
            LocalDate asOf
    ) {
        Loan loan = installment.getLoan();
        User collector = loan.getAssignedCollector();

        BigDecimal paid = installment.getPaidAmount() == null
                ? BigDecimal.ZERO
                : installment.getPaidAmount();

        BigDecimal remaining = installment.getPrincipalDue()
                .subtract(paid)
                .max(BigDecimal.ZERO);

        long days = ChronoUnit.DAYS.between(
                installment.getDueDate(),
                asOf
        );

        String bucket;

        if (days <= 7) {
            bucket = "DAYS_1_TO_7";
        } else if (days <= 30) {
            bucket = "DAYS_8_TO_30";
        } else {
            bucket = "DAYS_31_PLUS";
        }

        return new ManagerOverdueInstallmentResponse(
                installment.getInstallmentId(),
                loan.getLoanId(),
                loan.getLoanCode(),
                loan.getBorrower().getUser().getFullName(),
                loan.getBorrower().getUser().getPhone(),
                collector == null ? null : collector.getUserId(),
                collector == null ? null : collector.getFullName(),
                installment.getInstallmentNumber(),
                installment.getDueDate(),
                days,
                bucket,
                installment.getPrincipalDue(),
                paid,
                remaining
        );
    }
}
