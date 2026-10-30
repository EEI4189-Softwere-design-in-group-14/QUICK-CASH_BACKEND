package com.quickcash.controller;

import com.quickcash.dto.ReconciliationRequest;
import com.quickcash.dto.ReconciliationResponse;

import com.quickcash.service.ReconciliationService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/reconciliations")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerReconciliationController {

    private final ReconciliationService reconciliationService;

    public ManagerReconciliationController(
            ReconciliationService reconciliationService
    ) {
        this.reconciliationService =
                reconciliationService;
    }

    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>>
    preview(

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate date
    ) {

        BigDecimal total =
                reconciliationService
                        .getSystemCollectionTotal(date);

        return ResponseEntity.ok(
                Map.of(
                        "date", date,
                        "systemCollectionTotal", total
                )
        );
    }

    @PostMapping
    public ResponseEntity<ReconciliationResponse>
    reconcile(
            Authentication authentication,
            @Valid
            @RequestBody
            ReconciliationRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        reconciliationService
                                .reconcile(
                                        authentication.getName(),
                                        request
                                )
                );
    }

    @GetMapping
    public ResponseEntity<List<ReconciliationResponse>>
    getHistory() {

        return ResponseEntity.ok(
                reconciliationService
                        .getHistory()
        );
    }

    @GetMapping("/{date}")
    public ResponseEntity<ReconciliationResponse>
    getByDate(

            @PathVariable
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate date
    ) {

        return ResponseEntity.ok(
                reconciliationService
                        .getByDate(date)
        );
    }
}