package com.quickcash.dto;

import com.quickcash.entity.Disbursement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DisbursementResponse {

    private Long disbursementId;
    private Long loanId;
    private String loanCode;

    private BigDecimal amount;

    private String status;

    private LocalDateTime otpVerifiedAt;
    private LocalDateTime disbursedAt;

    private String confirmedBy;

    public DisbursementResponse(
            Disbursement disbursement
    ) {

        this.disbursementId =
                disbursement.getDisbursementId();

        this.loanId =
                disbursement.getLoan().getLoanId();

        this.loanCode =
                disbursement.getLoan().getLoanCode();

        this.amount =
                disbursement.getAmount();

        this.status =
                disbursement.getStatus().name();

        this.otpVerifiedAt =
                disbursement.getOtpVerifiedAt();

        this.disbursedAt =
                disbursement.getDisbursedAt();

        if (disbursement.getConfirmedBy() != null) {

            this.confirmedBy =
                    disbursement
                            .getConfirmedBy()
                            .getFullName();
        }
    }

    public Long getDisbursementId() {
        return disbursementId;
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

    public String getStatus() {
        return status;
    }

    public LocalDateTime getOtpVerifiedAt() {
        return otpVerifiedAt;
    }

    public LocalDateTime getDisbursedAt() {
        return disbursedAt;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }
}