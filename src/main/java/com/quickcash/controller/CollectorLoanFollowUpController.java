
package com.quickcash.controller;

import com.quickcash.dto.CreateLoanFollowUpRequest;
import com.quickcash.dto.LoanFollowUpResponse;
import com.quickcash.service.LoanFollowUpService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collector/loans/{loanId}/follow-ups")
@PreAuthorize("hasRole('COLLECTOR')")
public class CollectorLoanFollowUpController {

    private final LoanFollowUpService service;

    public CollectorLoanFollowUpController(
            LoanFollowUpService service
    ) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanFollowUpResponse create(
            @PathVariable Long loanId,
            @Valid @RequestBody CreateLoanFollowUpRequest request,
            Authentication authentication
    ) {
        return service.create(
                loanId,
                authentication.getName(),
                request
        );
    }

    @GetMapping
    public List<LoanFollowUpResponse> getHistory(
            @PathVariable Long loanId,
            Authentication authentication
    ) {
        return service.getForCollector(
                loanId,
                authentication.getName()
        );
    }
}
