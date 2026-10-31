
package com.quickcash.controller;

import com.quickcash.dto.CollectorAssignedLoanResponse;
import com.quickcash.service.CollectorAssignmentService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collector/loans")
@PreAuthorize("hasRole('COLLECTOR')")
public class CollectorAssignedLoanController {

    private final CollectorAssignmentService assignmentService;

    public CollectorAssignedLoanController(
            CollectorAssignmentService assignmentService
    ) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<CollectorAssignedLoanResponse> getMyLoans(
            Authentication authentication
    ) {
        return assignmentService.getMyAssignedLoans(
                authentication.getName()
        );
    }

    @GetMapping("/{loanId}")
    public CollectorAssignedLoanResponse getMyLoan(
            @PathVariable Long loanId,
            Authentication authentication
    ) {
        return assignmentService.getMyAssignedLoan(
                loanId,
                authentication.getName()
        );
    }
}
