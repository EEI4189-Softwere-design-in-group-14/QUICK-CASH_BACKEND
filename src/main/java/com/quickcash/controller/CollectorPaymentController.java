package com.quickcash.controller;

import com.quickcash.dto.PaymentRequest;
import com.quickcash.dto.PaymentResponse;

import com.quickcash.entity.PaymentSource;

import com.quickcash.service.PaymentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collector/payments")
@PreAuthorize("hasRole('COLLECTOR')")
public class CollectorPaymentController {

    private final PaymentService paymentService;

    public CollectorPaymentController(
            PaymentService paymentService
    ) {
        this.paymentService =
                paymentService;
    }

    /*
     * Normal online payment.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse>
    recordPayment(
            Authentication authentication,
            @Valid
            @RequestBody
            PaymentRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        paymentService.recordPayment(
                                authentication.getName(),
                                request,
                                PaymentSource.ONLINE
                        )
                );
    }

    /*
     * Payment originally recorded on the
     * Flutter device while offline.
     */
    @PostMapping("/sync")
    public ResponseEntity<PaymentResponse>
    syncOfflinePayment(
            Authentication authentication,
            @Valid
            @RequestBody
            PaymentRequest request
    ) {

        return ResponseEntity.ok(
                paymentService.recordPayment(
                        authentication.getName(),
                        request,
                        PaymentSource.OFFLINE_SYNC
                )
        );
    }


    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<PaymentResponse>> getLoanPayments(
            @PathVariable Long loanId,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                paymentService.getLoanPayments(
                        loanId,
                        authentication.getName()
                )
        );
    }

}