
package com.quickcash.dto;

import com.quickcash.entity.Payment;
import com.quickcash.entity.PaymentSource;
import com.quickcash.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BorrowerPaymentHistoryResponse(
        Long paymentId,
        String paymentCode,
        Long loanId,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        PaymentStatus status,
        PaymentSource source,
        LocalDateTime paymentDate
) {

    public static BorrowerPaymentHistoryResponse from(
            Payment payment
    ) {
        return new BorrowerPaymentHistoryResponse(
                payment.getPaymentId(),
                payment.getPaymentCode(),
                payment.getLoan().getLoanId(),
                payment.getAmount(),
                payment.getBalanceBefore(),
                payment.getBalanceAfter(),
                payment.getStatus(),
                payment.getSource(),
                payment.getPaymentDate()
        );
    }
}
