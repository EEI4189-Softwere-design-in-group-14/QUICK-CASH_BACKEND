
package com.quickcash.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CollectorDashboardResponse(
        LocalDate date,
        long assignedActiveLoans,
        BigDecimal totalOutstandingBalance,
        long todayPaymentCount,
        BigDecimal totalCollectedToday
) {
}
