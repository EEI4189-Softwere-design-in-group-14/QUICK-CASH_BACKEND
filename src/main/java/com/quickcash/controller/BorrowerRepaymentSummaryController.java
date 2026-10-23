
package com.quickcash.controller;

import com.quickcash.dto.RepaymentSummaryResponse;
import com.quickcash.service.RepaymentSummaryService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/borrower/loans")
@PreAuthorize("hasRole('BORROWER')")
public class BorrowerRepaymentSummaryController {

    private final RepaymentSummaryService repaymentSummaryService;

    public BorrowerRepaymentSummaryController(
            RepaymentSummaryService repaymentSummaryService
    ) {
        this.repaymentSummaryService = repaymentSummaryService;
    }

    @GetMapping("/{loanId}/repayment-summary")
    public RepaymentSummaryResponse getRepaymentSummary(
            @PathVariable Long loanId,
            Principal principal
    ) {

        return repaymentSummaryService.getMyRepaymentSummary(
                loanId,
                principal.getName()
        );
    }
}
