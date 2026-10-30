package com.quickcash.controller;

import com.quickcash.dto.BorrowerResponse;
import com.quickcash.dto.LoanResponse;
import com.quickcash.dto.PaymentResponse;

import com.quickcash.service.ManagerReportService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/manager/reports")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class ManagerReportController {

    private final ManagerReportService reportService;

    public ManagerReportController(
            ManagerReportService reportService
    ) {
        this.reportService = reportService;
    }

    @GetMapping("/active-loans")
    public ResponseEntity<List<LoanResponse>>
    getActiveLoans() {

        return ResponseEntity.ok(
                reportService.getActiveLoans()
        );
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponse>>
    getPayments() {

        return ResponseEntity.ok(
                reportService.getAllPayments()
        );
    }

    @GetMapping("/borrowers")
    public ResponseEntity<List<BorrowerResponse>>
    getBorrowers() {

        return ResponseEntity.ok(
                reportService.getAllBorrowers()
        );
    }

    @GetMapping("/payments/filter")
    public ResponseEntity<List<PaymentResponse>>
    getPaymentsByDateRange(

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate from,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate to
    ) {

        return ResponseEntity.ok(
                reportService
                        .getPaymentsByDateRange(
                                from,
                                to
                        )
        );
    }
}