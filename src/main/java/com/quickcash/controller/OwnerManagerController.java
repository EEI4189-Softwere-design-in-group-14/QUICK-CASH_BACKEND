
package com.quickcash.controller;

import com.quickcash.dto.CreateManagerRequest;
import com.quickcash.dto.ManagerResponse;
import com.quickcash.service.OwnerManagerService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import com.quickcash.dto.UpdateManagerStatusRequest;

import java.util.List;
@RestController
@RequestMapping("/api/owner/managers")
@PreAuthorize("hasRole('OWNER')")
public class OwnerManagerController {

    private final OwnerManagerService ownerManagerService;

    public OwnerManagerController(
            OwnerManagerService ownerManagerService
    ) {
        this.ownerManagerService = ownerManagerService;
    }

    @PostMapping
    public ResponseEntity<ManagerResponse> createManager(
            @Valid @RequestBody CreateManagerRequest request,
            Principal principal
    ) {

        ManagerResponse response =
                ownerManagerService.createManager(
                        request,
                        principal.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @GetMapping
    public List<ManagerResponse> getAllManagers() {
        return ownerManagerService.getAllManagers();
    }


    @PatchMapping("/{managerId}/status")
    public ManagerResponse updateManagerStatus(
            @PathVariable Long managerId,
            @Valid @RequestBody UpdateManagerStatusRequest request,
            Principal principal
    ) {
        return ownerManagerService.updateManagerStatus(
                managerId,
                request,
                principal.getName()
        );
    }

}
