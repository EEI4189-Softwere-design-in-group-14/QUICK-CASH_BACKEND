
package com.quickcash.controller;

import com.quickcash.dto.ManagerOverdueInstallmentResponse;
import com.quickcash.dto.ManagerOverdueSummaryResponse;
import com.quickcash.service.ManagerOverdueService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/manager/overdue")
@PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
public class ManagerOverdueController {

    private final ManagerOverdueService service;

    public ManagerOverdueController(
            ManagerOverdueService service
    ) {
        this.service = service;
    }

    @GetMapping("/installments")
    public List<ManagerOverdueInstallmentResponse> getInstallments(
            Authentication authentication,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate asOf
    ) {
        return service.getInstallments(
                authentication.getName(),
                asOf
        );
    }

    @GetMapping("/summary")
    public ManagerOverdueSummaryResponse getSummary(
            Authentication authentication,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate asOf
    ) {
        return service.getSummary(
                authentication.getName(),
                asOf
        );
    }
}
