
package com.quickcash.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CollectorDueSummaryResponse(
        LocalDate asOfDate,
        long dueOnDateCount,
        long overdueCount,
        BigDecimal totalRemainingDue
) {
}
