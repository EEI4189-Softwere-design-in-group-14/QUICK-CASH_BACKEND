
package com.quickcash.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RepaymentSummaryResponse(

        Long loanId,
        String loanCode,
        BigDecimal totalPrincipal,
        BigDecimal totalPaid,
        BigDecimal outstandingBalance,
        int totalInstallments,
        int paidInstallments,
        int remainingInstallments,
        Integer nextInstallmentNumber,
        LocalDate nextDueDate,
        BigDecimal nextInstallmentRemaining

) {
}
