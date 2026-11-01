
package com.quickcash.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LoanRepaymentCalculator {

    public LoanCalculation calculate(
            BigDecimal loanAmount,
            BigDecimal totalRepayment,
            int repaymentDays
    ) {

        // Step 1: Validate inputs
        if (loanAmount == null || totalRepayment == null) {
            throw new IllegalArgumentException(
                    "Loan amounts are required"
            );
        }

        if (loanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Loan amount must be positive"
            );
        }

        if (totalRepayment.compareTo(loanAmount) < 0) {
            throw new IllegalArgumentException(
                    "Total repayment cannot be less than loan amount"
            );
        }

        if (repaymentDays <= 0) {
            throw new IllegalArgumentException(
                    "Repayment days must be positive"
            );
        }

        // LKR amounts must not contain fractions of a cent.
        loanAmount = loanAmount.setScale(
                2, RoundingMode.UNNECESSARY
        );

        totalRepayment = totalRepayment.setScale(
                2, RoundingMode.UNNECESSARY
        );

        // Step 2: Calculate interest
        BigDecimal interestAmount =
                totalRepayment.subtract(loanAmount);

        // Step 3: Calculate interest percentage
        BigDecimal interestPercentage = interestAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(loanAmount, 2, RoundingMode.HALF_UP);

        // Step 4: Calculate daily installment
        BigDecimal dailyInstallment = totalRepayment.divide(
                BigDecimal.valueOf(repaymentDays),
                2,
                RoundingMode.DOWN
        );

        if (dailyInstallment.signum() == 0) {
            throw new IllegalArgumentException(
                    "Repayment amount is too small for the number of days"
            );
        }

        // Step 5: Correct rounding in the final installment
        BigDecimal lastInstallment = totalRepayment.subtract(
                dailyInstallment.multiply(
                        BigDecimal.valueOf(repaymentDays - 1)
                )
        );

        // Step 6: Return the calculated results
        return new LoanCalculation(
                interestAmount,
                interestPercentage,
                dailyInstallment,
                lastInstallment
        );
    }

    public record LoanCalculation(
            BigDecimal interestAmount,
            BigDecimal interestPercentage,
            BigDecimal dailyInstallment,
            BigDecimal lastInstallment
    ) {}
}
