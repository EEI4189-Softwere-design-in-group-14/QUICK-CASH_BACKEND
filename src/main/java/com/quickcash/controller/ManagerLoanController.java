package com.quickcash.controller;

import com.quickcash.dto.LoanApprovalRequest;
import com.quickcash.dto.LoanRejectionRequest;
import com.quickcash.dto.LoanResponse;
import com.quickcash.service.LoanService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/loans")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class ManagerLoanController {

    private final LoanService loanService;

    public ManagerLoanController(
            LoanService loanService
    ) {
        this.loanService = loanService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<LoanResponse>>
    getPendingLoans() {

        return ResponseEntity.ok(
                loanService.getPendingLoans()
        );
    }

    @PutMapping("/{loanId}/approve")
    public ResponseEntity<LoanResponse>
    approveLoan(
            @PathVariable Long loanId,
            Authentication authentication,
            @Valid
            @RequestBody
            LoanApprovalRequest request
    ) {

        return ResponseEntity.ok(
                loanService.approveLoan(
                        loanId,
                        authentication.getName(),
                        request
                )
        );
    }

    @PutMapping("/{loanId}/reject")
    public ResponseEntity<LoanResponse>
    rejectLoan(
            @PathVariable Long loanId,
            Authentication authentication,
            @Valid
            @RequestBody
            LoanRejectionRequest request
    ) {

        return ResponseEntity.ok(
                loanService.rejectLoan(
                        loanId,
                        authentication.getName(),
                        request
                )
        );
    }
}