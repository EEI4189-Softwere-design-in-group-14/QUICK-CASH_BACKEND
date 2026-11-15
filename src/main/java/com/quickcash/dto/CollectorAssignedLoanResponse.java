
package com.quickcash.dto;

import com.quickcash.entity.Loan;
import com.quickcash.entity.LoanStatus;

import java.math.BigDecimal;

public record CollectorAssignedLoanResponse(
        Long loanId,
        String loanCode,
        String borrowerName,
        BigDecimal approvedAmount,
        BigDecimal outstandingBalance,
        LoanStatus status,
        Long assignedCollectorUserId
) {

    public static CollectorAssignedLoanResponse from(
            Loan loan
    ) {
        return new CollectorAssignedLoanResponse(
                loan.getLoanId(),
                loan.getLoanCode(),
                loan.getBorrower().getUser().getFullName(),
                loan.getApprovedAmount(),
                loan.getOutstandingBalance(),
                loan.getStatus(),
                loan.getAssignedCollector() == null
                        ? null
                        : loan.getAssignedCollector().getUserId()
        );
    }
}
