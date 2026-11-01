
package com.quickcash.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DailyRepaymentScheduleCalculator {

    private final LoanRepaymentCalculator calculator =
            new LoanRepaymentCalculator();

    public record DailyInstallment(
            int installmentNumber,
            LocalDate dueDate,
            BigDecimal amountDue
    ) {}

    public List<DailyInstallment> createSchedule(
            BigDecimal loanAmount,
            BigDecimal totalRepayment,
            int repaymentDays,
            LocalDate disbursementDate
    ) {

        if (disbursementDate == null) {
            throw new IllegalArgumentException(
                    "Disbursement date is required"
            );
        }

        var calculation = calculator.calculate(
                loanAmount,
                totalRepayment,
                repaymentDays
        );

        List<DailyInstallment> schedule =
                new ArrayList<>();

        for (int day = 1; day <= repaymentDays; day++) {

            BigDecimal amount;

            if (day == repaymentDays) {
                amount = calculation.lastInstallment();
            } else {
                amount = calculation.dailyInstallment();
            }

            LocalDate dueDate =
                    disbursementDate.plusDays(day);

            schedule.add(
                    new DailyInstallment(
                            day,
                            dueDate,
                            amount
                    )
            );
        }

        return List.copyOf(schedule);
    }
}
