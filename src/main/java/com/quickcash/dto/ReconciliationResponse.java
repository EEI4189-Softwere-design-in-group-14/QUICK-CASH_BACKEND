package com.quickcash.dto;

import com.quickcash.entity.Reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReconciliationResponse {

    private Long reconciliationId;

    private LocalDate reconciliationDate;

    private BigDecimal systemCollectionTotal;
    private BigDecimal actualAmount;
    private BigDecimal difference;

    private String status;
    private String note;

    private String reconciledBy;
    private LocalDateTime reconciledAt;

    public ReconciliationResponse(
            Reconciliation reconciliation
    ) {

        this.reconciliationId =
                reconciliation.getReconciliationId();

        this.reconciliationDate =
                reconciliation.getReconciliationDate();

        this.systemCollectionTotal =
                reconciliation.getSystemCollectionTotal();

        this.actualAmount =
                reconciliation.getActualAmount();

        this.difference =
                reconciliation.getDifference();

        this.status =
                reconciliation.getStatus().name();

        this.note =
                reconciliation.getNote();

        this.reconciledBy =
                reconciliation
                        .getReconciledBy()
                        .getFullName();

        this.reconciledAt =
                reconciliation.getReconciledAt();
    }

    public Long getReconciliationId() {
        return reconciliationId;
    }

    public LocalDate getReconciliationDate() {
        return reconciliationDate;
    }

    public BigDecimal getSystemCollectionTotal() {
        return systemCollectionTotal;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public String getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public String getReconciledBy() {
        return reconciledBy;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }
}