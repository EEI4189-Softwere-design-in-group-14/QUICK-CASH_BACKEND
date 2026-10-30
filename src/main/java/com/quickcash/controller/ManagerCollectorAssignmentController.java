
package com.quickcash.controller;

import com.quickcash.dto.AssignCollectorRequest;
import com.quickcash.dto.CollectorAssignedLoanResponse;
import com.quickcash.service.CollectorAssignmentService;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/collector-assignments")
@PreAuthorize("hasAnyRole('MANAGER','OWNER')")
public class ManagerCollectorAssignmentController {

    private final CollectorAssignmentService assignmentService;

    public ManagerCollectorAssignmentController(
            CollectorAssignmentService assignmentService
    ) {
        this.assignmentService = assignmentService;
    }

    @PutMapping("/loans/{loanId}")
    public CollectorAssignedLoanResponse assignCollector(
            @PathVariable Long loanId,
            @Valid @RequestBody AssignCollectorRequest request,
            Authentication authentication
    ) {

        return assignmentService.assignCollector(
                loanId,
                request.collectorUserId(),
                authentication.getName()
        );
    }
}
