package com.quickcash.dto;

import com.quickcash.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {

    private Long paymentId;
    private String paymentCode;

    private String clientTransactionId;

    private Long loanId;
    private String loanCode;

    private BigDecimal amount;

    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;

    private String collector;

    private String source;
    private String status;

    private LocalDateTime paymentDate;

    public PaymentResponse(Payment payment) {

        this.paymentId =
                payment.getPaymentId();

        this.paymentCode =
                payment.getPaymentCode();

        this.clientTransactionId =
                payment.getClientTransactionId();

        this.loanId =
                payment.getLoan().getLoanId();

        this.loanCode =
                payment.getLoan().getLoanCode();

        this.amount =
                payment.getAmount();

        this.balanceBefore =
                payment.getBalanceBefore();

        this.balanceAfter =
                payment.getBalanceAfter();

        this.collector =
                payment.getCollector()
                        .getFullName();

        this.source =
                payment.getSource().name();

        this.status =
                payment.getStatus().name();

        this.paymentDate =
                payment.getPaymentDate();
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public String getClientTransactionId() {
        return clientTransactionId;
    }

    public Long getLoanId() {
        return loanId;
    }

    public String getLoanCode() {
        return loanCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getCollector() {
        return collector;
    }

    public String getSource() {
        return source;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }
}