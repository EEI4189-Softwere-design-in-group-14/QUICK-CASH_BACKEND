package com.quickcash.dto;

import java.time.LocalDateTime;

public class OtpRequestResponse {

    private Long disbursementId;
    private String message;
    private LocalDateTime expiresAt;

    public OtpRequestResponse(
            Long disbursementId,
            String message,
            LocalDateTime expiresAt
    ) {
        this.disbursementId = disbursementId;
        this.message = message;
        this.expiresAt = expiresAt;
    }

    public Long getDisbursementId() {
        return disbursementId;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}