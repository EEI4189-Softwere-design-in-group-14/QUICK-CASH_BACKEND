
package com.quickcash.controller;

import com.quickcash.dto.InstallmentResponse;
import com.quickcash.service.InstallmentQueryService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/borrower/loans")
@PreAuthorize("hasRole('BORROWER')")
public class BorrowerInstallmentController {

    private final InstallmentQueryService installmentQueryService;

    public BorrowerInstallmentController(
            InstallmentQueryService installmentQueryService
    ) {
        this.installmentQueryService = installmentQueryService;
    }

    @GetMapping("/{loanId}/installments")
    public List<InstallmentResponse> getInstallments(
            @PathVariable Long loanId,
            Principal principal
    ) {

        return installmentQueryService.getMyInstallments(
                loanId,
                principal.getName()
        );
    }
}
