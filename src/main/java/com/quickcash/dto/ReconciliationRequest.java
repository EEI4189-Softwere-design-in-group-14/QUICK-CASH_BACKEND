package com.quickcash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReconciliationRequest {

    @NotNull(
            message = "Reconciliation date is required"
    )
    private LocalDate reconciliationDate;

    @NotNull(
            message = "Actual amount is required"
    )
    @DecimalMin(
            value = "0.00",
            message = "Actual amount cannot be negative"
    )
    private BigDecimal actualAmount;

    @Size(max = 500)
    private String note;

    public ReconciliationRequest() {
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

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(
            BigDecimal actualAmount
    ) {
        this.actualAmount = actualAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}