package com.quickcash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class LoanApprovalRequest {

    @NotNull(message = "Approved amount is required")
    @DecimalMin(
            value = "1.00",
            message = "Approved amount must be greater than zero"
    )
    private BigDecimal approvedAmount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(
            value = "0.00",
            message = "Interest rate cannot be negative"
    )
    private BigDecimal interestRate;

    public LoanApprovalRequest() {
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }
}