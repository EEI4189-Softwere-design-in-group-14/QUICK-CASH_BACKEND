package com.quickcash.controller;

import com.quickcash.dto.DisbursementResponse;
import com.quickcash.dto.OtpRequestResponse;
import com.quickcash.dto.OtpVerifyRequest;

import com.quickcash.service.DisbursementService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/disbursements")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerDisbursementController {

    private final DisbursementService disbursementService;

    public ManagerDisbursementController(
            DisbursementService disbursementService
    ) {
        this.disbursementService =
                disbursementService;
    }

    @PostMapping("/loan/{loanId}/otp")
    public ResponseEntity<OtpRequestResponse>
    requestOtp(
            @PathVariable Long loanId
    ) {

        return ResponseEntity.ok(
                disbursementService
                        .requestOtp(loanId)
        );
    }

    @PostMapping("/{disbursementId}/confirm")
    public ResponseEntity<DisbursementResponse>
    confirmDisbursement(
            @PathVariable Long disbursementId,
            Authentication authentication,
            @Valid
            @RequestBody
            OtpVerifyRequest request
    ) {

        return ResponseEntity.ok(
                disbursementService
                        .confirmDisbursement(
                                disbursementId,
                                authentication.getName(),
                                request
                        )
        );
    }
}