package com.quickcash.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoanRejectionRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(max = 500)
    private String reason;

    public LoanRejectionRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}