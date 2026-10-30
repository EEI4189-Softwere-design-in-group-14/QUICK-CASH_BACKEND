package com.quickcash.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "disbursements")
public class Disbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long disbursementId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "loan_id",
            nullable = false,
            unique = true
    )
    private Loan loan;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;

    @Column(nullable = false)
    private String otpHash;

    @Column(nullable = false)
    private LocalDateTime otpExpiresAt;

    private LocalDateTime otpVerifiedAt;

    private LocalDateTime disbursedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisbursementStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Disbursement() {
    }

    public Long getDisbursementId() {
        return disbursementId;
    }

    public void setDisbursementId(Long disbursementId) {
        this.disbursementId = disbursementId;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public LocalDateTime getOtpExpiresAt() {
        return otpExpiresAt;
    }

    public void setOtpExpiresAt(LocalDateTime otpExpiresAt) {
        this.otpExpiresAt = otpExpiresAt;
    }

    public LocalDateTime getOtpVerifiedAt() {
        return otpVerifiedAt;
    }

    public void setOtpVerifiedAt(LocalDateTime otpVerifiedAt) {
        this.otpVerifiedAt = otpVerifiedAt;
    }

    public LocalDateTime getDisbursedAt() {
        return disbursedAt;
    }

    public void setDisbursedAt(LocalDateTime disbursedAt) {
        this.disbursedAt = disbursedAt;
    }

    public User getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(User confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public DisbursementStatus getStatus() {
        return status;
    }

    public void setStatus(DisbursementStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}