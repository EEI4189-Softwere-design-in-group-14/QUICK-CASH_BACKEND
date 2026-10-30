
package com.quickcash.service;

import com.quickcash.dto.CollectorAssignedLoanResponse;
import com.quickcash.entity.*;
import com.quickcash.repository.LoanRepository;
import com.quickcash.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CollectorAssignmentService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CollectorAssignmentService(
            LoanRepository loanRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // Manager or owner assigns a collector to a loan.
    @Transactional
    public CollectorAssignedLoanResponse assignCollector(
            Long loanId,
            Long collectorUserId,
            String managerEmail
    ) {

        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Manager not found"
                        )
                );

        if ((manager.getRole() != Role.MANAGER
                && manager.getRole() != Role.OWNER)
                || manager.getStatus() != UserStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only an active manager or owner can assign collectors"
            );
        }

        User collector = userRepository.findById(collectorUserId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Collector not found"
                        )
                );

        if (collector.getRole() != Role.COLLECTOR
                || collector.getStatus() != UserStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected user must be an active collector"
            );
        }

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Loan not found"
                        )
                );

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only active loans can be assigned"
            );
        }

        User previousCollector = loan.getAssignedCollector();

        // Repeating the same assignment does not create another audit.
        if (previousCollector != null
                && previousCollector.getUserId()
                .equals(collector.getUserId())) {

            return CollectorAssignedLoanResponse.from(loan);
        }

        AuditAction action = previousCollector == null
                ? AuditAction.COLLECTOR_ASSIGNED
                : AuditAction.COLLECTOR_REASSIGNED;

        loan.setAssignedCollector(collector);

        Loan savedLoan = loanRepository.save(loan);

        auditService.record(
                manager,
                action,
                "LOAN",
                savedLoan.getLoanId(),
                savedLoan.getLoanCode(),
                "Loan assigned to collector user ID "
                        + collector.getUserId()
        );

        return CollectorAssignedLoanResponse.from(savedLoan);
    }

    // Collector views only their assigned active loans.
    @Transactional(readOnly = true)
    public List<CollectorAssignedLoanResponse> getMyAssignedLoans(
            String collectorEmail
    ) {

        User collector = getActiveCollector(collectorEmail);

        return loanRepository
                .findByAssignedCollector_UserIdAndStatusOrderByLoanIdDesc(
                        collector.getUserId(),
                        LoanStatus.ACTIVE
                )
                .stream()
                .map(CollectorAssignedLoanResponse::from)
                .toList();
    }

    // Collector views one assigned active loan.
    @Transactional(readOnly = true)
    public CollectorAssignedLoanResponse getMyAssignedLoan(
            Long loanId,
            String collectorEmail
    ) {

        User collector = getActiveCollector(collectorEmail);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Assigned loan not found"
                        )
                );

        if (loan.getStatus() != LoanStatus.ACTIVE
                || loan.getAssignedCollector() == null
                || !loan.getAssignedCollector().getUserId()
                .equals(collector.getUserId())) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Assigned loan not found"
            );
        }

        return CollectorAssignedLoanResponse.from(loan);
    }

    private User getActiveCollector(String email) {

        User collector = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Collector account not found"
                        )
                );

        if (collector.getRole() != Role.COLLECTOR
                || collector.getStatus() != UserStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Active collector account required"
            );
        }

        return collector;
    }
}
