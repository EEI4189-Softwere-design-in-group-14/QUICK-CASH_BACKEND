
package com.quickcash.controller;

import com.quickcash.dto.BorrowerPaymentHistoryResponse;
import com.quickcash.dto.ReceiptResponse;

import com.quickcash.service.BorrowerPaymentHistoryService;
import com.quickcash.service.PdfReceiptService;
import com.quickcash.service.ReceiptService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrower")
@PreAuthorize("hasRole('BORROWER')")
public class BorrowerPaymentHistoryController {

    private final BorrowerPaymentHistoryService paymentHistoryService;
    private final ReceiptService receiptService;
    private final PdfReceiptService pdfReceiptService;

    public BorrowerPaymentHistoryController(
            BorrowerPaymentHistoryService paymentHistoryService,
            ReceiptService receiptService,
            PdfReceiptService pdfReceiptService
    ) {
        this.paymentHistoryService = paymentHistoryService;
        this.receiptService = receiptService;
        this.pdfReceiptService = pdfReceiptService;
    }

    // 1. View all payments for the logged-in borrower
    @GetMapping("/payments")
    public List<BorrowerPaymentHistoryResponse> getMyPayments(
            Authentication authentication
    ) {
        return paymentHistoryService.getMyPayments(
                authentication.getName()
        );
    }

    // 2. View payments for a specific loan
    @GetMapping("/loans/{loanId}/payments")
    public List<BorrowerPaymentHistoryResponse> getMyLoanPayments(
            @PathVariable Long loanId,
            Authentication authentication
    ) {
        return paymentHistoryService.getMyLoanPayments(
                loanId,
                authentication.getName()
        );
    }

    // 3. Download PDF receipt for an owned payment
    @GetMapping(
            value = "/payments/{paymentId}/receipt",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> downloadMyReceipt(
            @PathVariable Long paymentId,
            Authentication authentication
    ) {

        // Secure lookup: verifies payment ownership
        ReceiptResponse receipt =
                receiptService.getReceiptByPayment(
                        paymentId,
                        authentication.getName()
                );

        // Generate the PDF using the existing service
        byte[] pdf = pdfReceiptService.generateReceiptPdf(
                receipt
        );

        String filename = receipt.getReceiptNumber() + ".pdf";

        String disposition = ContentDisposition
                .attachment()
                .filename(filename)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition
                )
                .contentLength(pdf.length)
                .body(pdf);
    }
}

