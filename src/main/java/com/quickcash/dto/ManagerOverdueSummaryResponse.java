
package com.quickcash.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ManagerOverdueSummaryResponse(
        LocalDate asOfDate,

        long overdueInstallmentCount,
        long overdueLoanCount,
        BigDecimal totalOverdueAmount,

        long days1To7Count,
        BigDecimal days1To7Amount,

        long days8To30Count,
        BigDecimal days8To30Amount,

        long days31PlusCount,
        BigDecimal days31PlusAmount
) {
}
