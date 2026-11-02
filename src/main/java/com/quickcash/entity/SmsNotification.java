
package com.quickcash.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "sms_notifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sms_payment",
                        columnNames = "payment_id"
                )
        }
)
public class SmsNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long smsId;

    // One payment = one borrower SMS record
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "payment_id",
            nullable = false,
            unique = true
    )
    private Payment payment;

    @Column(
            name = "recipient_phone",
            nullable = false,
            length = 20
    )
    private String recipientPhone;

    @Column(
            name = "message_body",
            nullable = false,
            length = 1000
    )
    private String messageBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SmsNotificationStatus status =
            SmsNotificationStatus.PENDING;

    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(length = 500)
    private String lastError;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public SmsNotification() {
    }

    public Long getSmsId() {
        return smsId;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public SmsNotificationStatus getStatus() {
        return status;
    }

    public void setStatus(SmsNotificationStatus status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
