
package com.quickcash.dto;

import java.math.BigDecimal;

public record CollectorBorrowerSearchResponse(
        Long borrowerId,
        String borrowerCode,
        String borrowerName,
        String phone,
        Long loanId,
        String loanCode,
        BigDecimal outstandingBalance
) {
}

