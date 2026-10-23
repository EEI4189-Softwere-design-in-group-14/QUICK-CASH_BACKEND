
package com.quickcash.service;

import com.quickcash.dto.InstallmentResponse;
import com.quickcash.entity.Loan;
import com.quickcash.repository.LoanInstallmentRepository;
import com.quickcash.repository.LoanRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class InstallmentQueryService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;

    public InstallmentQueryService(
            LoanRepository loanRepository,
            LoanInstallmentRepository installmentRepository
    ) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
    }

    @Transactional(readOnly = true)
    public List<InstallmentResponse> getMyInstallments(
            Long loanId,
            String borrowerEmail
    ) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Loan not found"
                        )
                );

        // Security: Borrowers can view only their own loans.
        String ownerEmail = loan.getBorrower()
                .getUser()
                .getEmail();

        if (!ownerEmail.equalsIgnoreCase(borrowerEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Loan not found"
            );
        }

        LocalDate today = LocalDate.now(
                ZoneId.of("Asia/Colombo")
        );

        return installmentRepository
                .findByLoanLoanIdOrderByInstallmentNumberAsc(
                        loanId
                )
                .stream()
                .map(installment ->
                        new InstallmentResponse(installment, today)
                )
                .toList();
    }
}
