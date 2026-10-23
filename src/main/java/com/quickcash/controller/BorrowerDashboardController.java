
package com.quickcash.controller;

import com.quickcash.dto.BorrowerDashboardResponse;
import com.quickcash.service.BorrowerDashboardService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/borrower")
@PreAuthorize("hasRole('BORROWER')")
public class BorrowerDashboardController {

    private final BorrowerDashboardService dashboardService;

    public BorrowerDashboardController(
            BorrowerDashboardService dashboardService
    ) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public BorrowerDashboardResponse getDashboard(
            Principal principal
    ) {
        return dashboardService.getDashboard(
                principal.getName()
        );
    }
}
