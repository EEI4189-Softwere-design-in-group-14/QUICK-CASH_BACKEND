package com.quickcash.dto;

import com.quickcash.entity.Payment;
import com.quickcash.entity.Receipt;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReceiptResponse {

    private Long receiptId;
    private String receiptNumber;

    private Long paymentId;
    private String paymentCode;

    private Long loanId;
    private String loanCode;

    private String borrowerCode;
    private String borrowerName;

    private BigDecimal amount;
    private BigDecimal balanceAfter;

    private String collectorName;

    private LocalDateTime paymentDate;
    private LocalDateTime issuedAt;

    public ReceiptResponse(Receipt receipt) {

        Payment payment = receipt.getPayment();

        this.receiptId =
                receipt.getReceiptId();

        this.receiptNumber =
                receipt.getReceiptNumber();

        this.paymentId =
                payment.getPaymentId();

        this.paymentCode =
                payment.getPaymentCode();

        this.loanId =
                payment.getLoan().getLoanId();

        this.loanCode =
                payment.getLoan().getLoanCode();

        this.borrowerCode =
                payment.getLoan()
                        .getBorrower()
                        .getBorrowerCode();

        this.borrowerName =
                payment.getLoan()
                        .getBorrower()
                        .getUser()
                        .getFullName();

        this.amount =
                payment.getAmount();

        this.balanceAfter =
                payment.getBalanceAfter();

        this.collectorName =
                payment.getCollector()
                        .getFullName();

        this.paymentDate =
                payment.getPaymentDate();

        this.issuedAt =
                receipt.getIssuedAt();
    }

    public Long getReceiptId() {
        return receiptId;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public Long getLoanId() {
        return loanId;
    }

    public String getLoanCode() {
        return loanCode;
    }

    public String getBorrowerCode() {
        return borrowerCode;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getCollectorName() {
        return collectorName;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }
}