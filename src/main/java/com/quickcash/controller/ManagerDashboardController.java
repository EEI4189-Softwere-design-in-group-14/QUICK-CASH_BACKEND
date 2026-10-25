package com.quickcash.controller;

import com.quickcash.dto.ManagerDashboardResponse;
import com.quickcash.service.ManagerDashboardService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/dashboard")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class ManagerDashboardController {

    private final ManagerDashboardService dashboardService;

    public ManagerDashboardController(
            ManagerDashboardService dashboardService
    ) {
        this.dashboardService =
                dashboardService;
    }

    @GetMapping
    public ResponseEntity<ManagerDashboardResponse>
    getDashboard() {

        return ResponseEntity.ok(
                dashboardService.getDashboard()
        );
    }
}