package com.quickcash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import jakarta.validation.constraints.Positive;

public class LoanApprovalRequest {

    @NotNull(message = "Approved amount is required")
    @DecimalMin(
            value = "1.00",
            message = "Approved amount must be greater than zero"
    )
    private BigDecimal approvedAmount;

        @DecimalMin(
            value = "0.00",
            message = "Interest rate cannot be negative"
    )
    private BigDecimal interestRate;


    // MONTHLY_PRINCIPAL_ONLY or DAILY_FIXED_TOTAL
    private String repaymentPlanType;

    // Total amount including interest
    @DecimalMin("0.01")
    private BigDecimal totalRepaymentAmount;

    // Number of repayment days
    @Positive
    private Integer repaymentDays;


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


    public String getRepaymentPlanType() {
        return repaymentPlanType;
    }

    public void setRepaymentPlanType(String repaymentPlanType) {
        this.repaymentPlanType = repaymentPlanType;
    }

    public BigDecimal getTotalRepaymentAmount() {
        return totalRepaymentAmount;
    }

    public void setTotalRepaymentAmount(
            BigDecimal totalRepaymentAmount
    ) {
        this.totalRepaymentAmount = totalRepaymentAmount;
    }

    public Integer getRepaymentDays() {
        return repaymentDays;
    }

    public void setRepaymentDays(Integer repaymentDays) {
        this.repaymentDays = repaymentDays;
    }

}