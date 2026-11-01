
package com.quickcash.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DailyRepaymentScheduleCalculatorTest {

    @Test
    void shouldGenerateDailySchedule() {

        DailyRepaymentScheduleCalculator calculator =
                new DailyRepaymentScheduleCalculator();

        var schedule = calculator.createSchedule(
                new BigDecimal("10000.00"),
                new BigDecimal("12000.00"),
                90,
                LocalDate.of(2026, 10, 31)
        );

        // Check total number of installments
        assertEquals(90, schedule.size());

        // Check first due date
        assertEquals(
                LocalDate.of(2026, 11, 1),
                schedule.get(0).dueDate()
        );

        // Check last due date
        assertEquals(
                LocalDate.of(2027, 1, 29),
                schedule.get(89).dueDate()
        );

        // Check daily amount
        assertEquals(
                new BigDecimal("133.33"),
                schedule.get(0).amountDue()
        );

        // Check final installment
        assertEquals(
                new BigDecimal("133.63"),
                schedule.get(89).amountDue()
        );

        // Check full repayment total
        BigDecimal total = schedule.stream()
                .map(
                        DailyRepaymentScheduleCalculator
                                .DailyInstallment::amountDue
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(
                0,
                new BigDecimal("12000.00")
                        .compareTo(total)
        );
    }
}
