package com.quickcash.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "reconciliations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reconciliation_date",
                        columnNames = "reconciliation_date"
                )
        }
)
public class Reconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reconciliationId;

    @Column(
            name = "reconciliation_date",
            nullable = false,
            unique = true
    )
    private LocalDate reconciliationDate;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal systemCollectionTotal;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal actualAmount;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal difference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus status;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reconciled_by",
            nullable = false
    )
    private User reconciledBy;

    @Column(nullable = false)
    private LocalDateTime reconciledAt;

    public Reconciliation() {
    }

    @PrePersist
    public void onCreate() {

        if (reconciledAt == null) {
            reconciledAt = LocalDateTime.now();
        }
    }

    public Long getReconciliationId() {
        return reconciliationId;
    }

    public void setReconciliationId(Long reconciliationId) {
        this.reconciliationId = reconciliationId;
    }

    public LocalDate getReconciliationDate() {
        return reconciliationDate;
    }

    public void setReconciliationDate(
            LocalDate reconciliationDate
    ) {
        this.reconciliationDate =
                reconciliationDate;
    }

    public BigDecimal getSystemCollectionTotal() {
        return systemCollectionTotal;
    }

    public void setSystemCollectionTotal(
            BigDecimal systemCollectionTotal
    ) {
        this.systemCollectionTotal =
                systemCollectionTotal;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(
            BigDecimal actualAmount
    ) {
        this.actualAmount = actualAmount;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public void setDifference(
            BigDecimal difference
    ) {
        this.difference = difference;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public void setStatus(
            ReconciliationStatus status
    ) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public User getReconciledBy() {
        return reconciledBy;
    }

    public void setReconciledBy(
            User reconciledBy
    ) {
        this.reconciledBy = reconciledBy;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(
            LocalDateTime reconciledAt
    ) {
        this.reconciledAt = reconciledAt;
    }
}