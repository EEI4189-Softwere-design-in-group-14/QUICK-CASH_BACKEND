package com.quickcash.controller;

import com.quickcash.dto.ReceiptResponse;
import com.quickcash.service.PdfReceiptService;
import com.quickcash.service.ReceiptService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/receipts")
@PreAuthorize(
        "hasAnyRole('BORROWER','COLLECTOR','MANAGER')"
)
public class ReceiptController {

    private final ReceiptService receiptService;
    private final PdfReceiptService pdfReceiptService;

    public ReceiptController(
            ReceiptService receiptService,
            PdfReceiptService pdfReceiptService
    ) {
        this.receiptService =
                receiptService;

        this.pdfReceiptService =
                pdfReceiptService;
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<ReceiptResponse>
    getReceiptByPayment(
            @PathVariable Long paymentId,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                receiptService
                        .getReceiptByPayment(
                                paymentId,
                                authentication.getName()
                        )
        );
    }

    @GetMapping("/{receiptId}/pdf")
    public ResponseEntity<byte[]>
    downloadReceiptPdf(
            @PathVariable Long receiptId,
            Authentication authentication
    ) {

        ReceiptResponse receipt =
                receiptService
                        .getReceiptById(
                                receiptId,
                                authentication.getName()
                        );

        byte[] pdf =
                pdfReceiptService
                        .generateReceiptPdf(receipt);

        return ResponseEntity
                .ok()
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + receipt.getReceiptNumber()
                                + ".pdf\""
                )
                .body(pdf);
    }
}