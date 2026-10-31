
package com.quickcash.controller;

import com.quickcash.dto.LoanFollowUpResponse;
import com.quickcash.service.LoanFollowUpService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/loans/{loanId}/follow-ups")
@PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
public class ManagerLoanFollowUpController {

    private final LoanFollowUpService service;

    public ManagerLoanFollowUpController(
            LoanFollowUpService service
    ) {
        this.service = service;
    }

    @GetMapping
    public List<LoanFollowUpResponse> getHistory(
            @PathVariable Long loanId,
            Authentication authentication
    ) {
        return service.getForManager(
                loanId,
                authentication.getName()
        );
    }
}
