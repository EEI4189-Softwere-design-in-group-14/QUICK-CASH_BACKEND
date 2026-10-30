
package com.quickcash.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(
        name = "installment_reminder_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_installment_reminder",
                        columnNames = {
                                "installment_id",
                                "reminder_type"
                        }
                )
        }
)
public class InstallmentReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reminderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "installment_id",
            nullable = false
    )
    private LoanInstallment installment;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "reminder_type",
            nullable = false,
            length = 50
    )
    private NotificationType reminderType;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @PrePersist
    public void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now(
                    ZoneId.of("Asia/Colombo")
            );
        }
    }

    public Long getReminderId() {
        return reminderId;
    }

    public LoanInstallment getInstallment() {
        return installment;
    }

    public void setInstallment(LoanInstallment installment) {
        this.installment = installment;
    }

    public NotificationType getReminderType() {
        return reminderType;
    }

    public void setReminderType(NotificationType reminderType) {
        this.reminderType = reminderType;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
