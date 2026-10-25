package com.quickcash.controller;

import com.quickcash.dto.BorrowerResponse;
import com.quickcash.service.BorrowerService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/borrowers")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
public class ManagerBorrowerController {

    private final BorrowerService borrowerService;

    public ManagerBorrowerController(
            BorrowerService borrowerService
    ) {
        this.borrowerService = borrowerService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<BorrowerResponse>>
    getPendingBorrowers() {

        return ResponseEntity.ok(
                borrowerService.getPendingBorrowers()
        );
    }

    @PutMapping("/{borrowerId}/approve")
    public ResponseEntity<BorrowerResponse>
    approveBorrower(
            @PathVariable Long borrowerId
    ) {

        return ResponseEntity.ok(
                borrowerService.approveBorrower(borrowerId)
        );
    }

    @PutMapping("/{borrowerId}/reject")
    public ResponseEntity<BorrowerResponse>
    rejectBorrower(
            @PathVariable Long borrowerId
    ) {

        return ResponseEntity.ok(
                borrowerService.rejectBorrower(borrowerId)
        );
    }
}