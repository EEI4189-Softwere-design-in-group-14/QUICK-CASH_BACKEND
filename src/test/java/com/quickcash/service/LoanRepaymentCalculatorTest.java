
package com.quickcash.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LoanRepaymentCalculatorTest {

    @Test
    void shouldCalculateLoanInterestAndDailyInstallment() {

        LoanRepaymentCalculator calculator =
                new LoanRepaymentCalculator();

        var result = calculator.calculate(
                new BigDecimal("10000.00"),
                new BigDecimal("12000.00"),
                100
        );

        assertEquals(
                new BigDecimal("2000.00"),
                result.interestAmount()
        );

        assertEquals(
                new BigDecimal("20.00"),
                result.interestPercentage()
        );

        assertEquals(
                new BigDecimal("120.00"),
                result.dailyInstallment()
        );

        assertEquals(
                new BigDecimal("120.00"),
                result.lastInstallment()
        );
    }
}
