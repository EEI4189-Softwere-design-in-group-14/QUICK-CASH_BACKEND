
package com.quickcash.controller;

import com.quickcash.dto.BorrowerLoanDetailsResponse;
import com.quickcash.service.BorrowerLoanQueryService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/borrower/loans")
@PreAuthorize("hasRole('BORROWER')")
public class BorrowerLoanHistoryController {

    private final BorrowerLoanQueryService loanQueryService;

    public BorrowerLoanHistoryController(
            BorrowerLoanQueryService loanQueryService
    ) {
        this.loanQueryService = loanQueryService;
    }

    @GetMapping("/{loanId}")
    public BorrowerLoanDetailsResponse getMyLoanDetails(
            @PathVariable Long loanId,
            Principal principal
    ) {
        return loanQueryService.getMyLoanDetails(
                loanId,
                principal.getName()
        );
    }
}
