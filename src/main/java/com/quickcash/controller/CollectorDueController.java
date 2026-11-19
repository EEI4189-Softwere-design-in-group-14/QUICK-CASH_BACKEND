
package com.quickcash.controller;

import com.quickcash.dto.CollectorDueInstallmentResponse;
import com.quickcash.dto.CollectorDueSummaryResponse;
import com.quickcash.service.CollectorDueService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/collector/collections/due")
@PreAuthorize("hasRole('COLLECTOR')")
public class CollectorDueController {

    private final CollectorDueService collectorDueService;

    public CollectorDueController(
            CollectorDueService collectorDueService
    ) {
        this.collectorDueService = collectorDueService;
    }

    @GetMapping
    public List<CollectorDueInstallmentResponse> getDueInstallments(
            Authentication authentication,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate asOf
    ) {
        return collectorDueService.getDueInstallments(
                authentication.getName(),
                asOf
        );
    }

    @GetMapping("/summary")
    public CollectorDueSummaryResponse getDueSummary(
            Authentication authentication,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate asOf
    ) {
        return collectorDueService.getDueSummary(
                authentication.getName(),
                asOf
        );
    }
}
