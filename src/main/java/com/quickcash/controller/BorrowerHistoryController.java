package com.quickcash.controller;

import com.quickcash.dto.PaymentResponse;
import com.quickcash.dto.ReceiptResponse;

import com.quickcash.service.PaymentService;
import com.quickcash.service.ReceiptService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrower/history")
@PreAuthorize("hasRole('BORROWER')")
public class BorrowerHistoryController {

    private final PaymentService paymentService;
    private final ReceiptService receiptService;

    public BorrowerHistoryController(
            PaymentService paymentService,
            ReceiptService receiptService
    ) {
        this.paymentService = paymentService;
        this.receiptService = receiptService;
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponse>>
    getMyPayments(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                paymentService.getMyPayments(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/receipts")
    public ResponseEntity<List<ReceiptResponse>>
    getMyReceipts(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                receiptService.getMyReceipts(
                        authentication.getName()
                )
        );
    }
}