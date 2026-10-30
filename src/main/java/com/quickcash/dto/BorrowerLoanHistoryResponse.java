
package com.quickcash.dto;

import com.quickcash.entity.Loan;
import java.math.BigDecimal;

public record BorrowerLoanHistoryResponse(

        Long loanId,
        String loanCode,
        String status,
        String purpose,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        BigDecimal outstandingBalance,
        String applicationDate,
        String rejectionReason

) {

    public static BorrowerLoanHistoryResponse from(Loan loan) {

        return new BorrowerLoanHistoryResponse(
                loan.getLoanId(),
                loan.getLoanCode(),
                String.valueOf(loan.getStatus()),
                loan.getPurpose(),
                loan.getRequestedAmount(),
                loan.getApprovedAmount(),
                loan.getOutstandingBalance(),
                loan.getApplicationDate() == null
                        ? null
                        : loan.getApplicationDate().toString(),
                loan.getRejectionReason()
        );
    }
}
