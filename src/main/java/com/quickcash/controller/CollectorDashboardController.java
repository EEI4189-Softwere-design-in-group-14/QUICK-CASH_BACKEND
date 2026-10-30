
package com.quickcash.controller;

import com.quickcash.dto.CollectorDashboardResponse;
import com.quickcash.dto.PaymentResponse;
import com.quickcash.service.CollectorDashboardService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collector")
@PreAuthorize("hasRole('COLLECTOR')")
public class CollectorDashboardController {

    private final CollectorDashboardService dashboardService;

    public CollectorDashboardController(
            CollectorDashboardService dashboardService
    ) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public CollectorDashboardResponse getDashboard(
            Authentication authentication
    ) {
        return dashboardService.getDashboard(
                authentication.getName()
        );
    }

    @GetMapping("/collections/today")
    public List<PaymentResponse> getTodayCollections(
            Authentication authentication
    ) {
        return dashboardService.getTodayCollections(
                authentication.getName()
        );
    }
}
