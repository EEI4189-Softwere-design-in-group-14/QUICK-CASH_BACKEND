
package com.quickcash.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BorrowerDashboardResponse(

        String borrowerEmail,
        int totalLoans,
        int activeLoans,
        BigDecimal activePrincipal,
        BigDecimal activePaid,
        BigDecimal activeOutstanding,
        long unreadNotifications,
        List<LoanCard> loans

) {

    public record LoanCard(

            Long loanId,
            String loanCode,
            String status,
            BigDecimal approvedPrincipal,
            BigDecimal paidPrincipal,
            BigDecimal outstandingPrincipal,
            Integer nextInstallmentNumber,
            LocalDate nextDueDate,
            BigDecimal nextInstallmentRemaining

    ) {
    }
}
