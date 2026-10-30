package com.quickcash.controller;

import com.quickcash.dto.LoanApplicationRequest;
import com.quickcash.dto.LoanResponse;
import com.quickcash.service.LoanService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrower/loans")
@PreAuthorize("hasRole('BORROWER')")
public class BorrowerLoanController {

    private final LoanService loanService;

    public BorrowerLoanController(
            LoanService loanService
    ) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanResponse>
    applyForLoan(
            Authentication authentication,
            @Valid
            @RequestBody
            LoanApplicationRequest request
    ) {

        LoanResponse response =
                loanService.applyForLoan(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<LoanResponse>>
    getMyLoans(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                loanService.getMyLoans(
                        authentication.getName()
                )
        );
    }
}