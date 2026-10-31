
package com.quickcash.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(
        name = "loan_follow_ups",
        indexes = {
                @Index(
                        name = "idx_followup_loan_created",
                        columnList = "loan_id,created_at"
                )
        }
)
public class LoanFollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collector_user_id", nullable = false)
    private User collector;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FollowUpOutcome outcome;

    @Column(nullable = false, length = 500)
    private String note;

    private LocalDate nextFollowUpDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now(
                ZoneId.of("Asia/Colombo")
        );
    }

    public Long getNoteId() {
        return noteId;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public User getCollector() {
        return collector;
    }

    public void setCollector(User collector) {
        this.collector = collector;
    }

    public FollowUpOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(FollowUpOutcome outcome) {
        this.outcome = outcome;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDate getNextFollowUpDate() {
        return nextFollowUpDate;
    }

    public void setNextFollowUpDate(LocalDate date) {
        this.nextFollowUpDate = date;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
