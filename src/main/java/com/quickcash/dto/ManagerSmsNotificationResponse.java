
package com.quickcash.dto;

import com.quickcash.entity.SmsNotificationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ManagerSmsNotificationResponse(
        Long smsId,
        Long paymentId,
        String paymentCode,
        Long loanId,
        String loanCode,
        BigDecimal paymentAmount,
        SmsNotificationStatus status,
        int attemptCount,
        LocalDateTime createdAt,
        LocalDateTime processingStartedAt,
        LocalDateTime sentAt,
        String lastError
) {}
