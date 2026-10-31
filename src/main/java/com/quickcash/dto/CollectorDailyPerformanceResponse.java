
package com.quickcash.dto;

import com.quickcash.entity.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CollectorDailyPerformanceResponse(

        LocalDate date,

        Long collectorUserId,
        String collectorName,
        UserStatus collectorStatus,

        long assignedActiveLoans,

        long paymentCount,
        long onlinePaymentCount,
        long offlinePaymentCount,

        BigDecimal totalCollected,
        BigDecimal onlineCollected,
        BigDecimal offlineCollected

) {
}
