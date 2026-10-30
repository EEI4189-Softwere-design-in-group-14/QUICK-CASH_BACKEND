
package com.quickcash.dto;

import java.util.List;

public record BorrowerLoanDetailsResponse(

        BorrowerLoanHistoryResponse loan,
        Integer repaymentMonths,
        String interestRate,
        String reviewedAt,
        List<InstallmentResponse> installments

) {
}
