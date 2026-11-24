
package com.quickcash.controller;

import com.quickcash.dto.CollectorDailyPerformanceResponse;
import com.quickcash.service.ManagerCollectorPerformanceService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/manager/collectors")
@PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
public class ManagerCollectorPerformanceController {

    private final ManagerCollectorPerformanceService service;

    public ManagerCollectorPerformanceController(
            ManagerCollectorPerformanceService service
    ) {
        this.service = service;
    }

    @GetMapping("/performance")
    public List<CollectorDailyPerformanceResponse>
    getAllCollectorPerformance(

            Authentication authentication,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return service.getAll(
                authentication.getName(),
                date
        );
    }

    @GetMapping("/{collectorId}/performance")
    public CollectorDailyPerformanceResponse
    getOneCollectorPerformance(

            @PathVariable Long collectorId,

            Authentication authentication,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return service.getOne(
                authentication.getName(),
                collectorId,
                date
        );
    }
}
