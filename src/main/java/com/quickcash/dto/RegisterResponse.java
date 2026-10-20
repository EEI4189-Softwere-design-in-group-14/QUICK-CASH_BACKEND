package com.quickcash.dto;

public class RegisterResponse {

    private Long userId;
    private String message;
    private String status;

    public RegisterResponse(Long userId, String message, String status) {
        this.userId = userId;
        this.message = message;
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }
}