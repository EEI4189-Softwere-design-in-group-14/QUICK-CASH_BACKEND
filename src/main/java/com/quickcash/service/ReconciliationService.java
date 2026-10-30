package com.quickcash.service;

import com.quickcash.dto.ReconciliationRequest;
import com.quickcash.dto.ReconciliationResponse;

import com.quickcash.entity.Reconciliation;
import com.quickcash.entity.ReconciliationStatus;
import com.quickcash.entity.Role;
import com.quickcash.entity.User;

import com.quickcash.repository.PaymentRepository;
import com.quickcash.repository.ReconciliationRepository;
import com.quickcash.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.quickcash.entity.AuditAction;

@Service
public class ReconciliationService {

    private final ReconciliationRepository reconciliationRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ReconciliationService(
            ReconciliationRepository reconciliationRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {

        this.reconciliationRepository =
                reconciliationRepository;

        this.paymentRepository =
                paymentRepository;

        this.userRepository =
                userRepository;

        this.auditService =
                auditService;
    }

    @Transactional
    public ReconciliationResponse reconcile(
            String managerEmail,
            ReconciliationRequest request
    ) {

        if (request.getReconciliationDate()
                .isAfter(LocalDate.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reconciliation date cannot be in the future"
            );
        }

        if (reconciliationRepository
                .existsByReconciliationDate(
                        request.getReconciliationDate()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Reconciliation already exists for this date"
            );
        }

        User manager =
                userRepository
                        .findByEmail(managerEmail)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Manager not found"
                                )
                        );

        if (manager.getRole() != Role.MANAGER) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only managers can perform reconciliation"
            );
        }

        LocalDateTime start =
                request.getReconciliationDate()
                        .atStartOfDay();

        LocalDateTime end =
                request.getReconciliationDate()
                        .plusDays(1)
                        .atStartOfDay();

        BigDecimal systemTotal =
                paymentRepository
                        .getCollectedAmountBetween(
                                start,
                                end
                        );

        if (systemTotal == null) {
            systemTotal = BigDecimal.ZERO;
        }

        BigDecimal actualAmount =
                request.getActualAmount();

        BigDecimal difference =
                actualAmount.subtract(
                        systemTotal
                );

        ReconciliationStatus status =
                difference.compareTo(
                        BigDecimal.ZERO
                ) == 0
                        ? ReconciliationStatus.MATCHED
                        : ReconciliationStatus.MISMATCH;

        Reconciliation reconciliation =
                new Reconciliation();

        reconciliation.setReconciliationDate(
                request.getReconciliationDate()
        );

        reconciliation.setSystemCollectionTotal(
                systemTotal
        );

        reconciliation.setActualAmount(
                actualAmount
        );

        reconciliation.setDifference(
                difference
        );

        reconciliation.setStatus(status);

        reconciliation.setNote(
                request.getNote() != null
                        ? request.getNote().trim()
                        : null
        );

        reconciliation.setReconciledBy(
                manager
        );

        Reconciliation saved =
                reconciliationRepository
                        .save(reconciliation);

        auditService.record(
                manager,
                AuditAction.RECONCILIATION_CREATED,
                "RECONCILIATION",
                saved.getReconciliationId(),
                saved.getReconciliationDate().toString(),
                "Bank reconciliation completed with status "
                        + saved.getStatus().name()
        );

        return new ReconciliationResponse(
                saved
        );
    }

    @Transactional(readOnly = true)
    public List<ReconciliationResponse>
    getHistory() {

        return reconciliationRepository
                .findAllByOrderByReconciliationDateDesc()
                .stream()
                .map(ReconciliationResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReconciliationResponse getByDate(
            LocalDate date
    ) {

        Reconciliation reconciliation =
                reconciliationRepository
                        .findByReconciliationDate(date)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Reconciliation not found for this date"
                                )
                        );

        return new ReconciliationResponse(
                reconciliation
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal getSystemCollectionTotal(
            LocalDate date
    ) {

        LocalDateTime start =
                date.atStartOfDay();

        LocalDateTime end =
                date.plusDays(1)
                        .atStartOfDay();

        BigDecimal total =
                paymentRepository
                        .getCollectedAmountBetween(
                                start,
                                end
                        );

        return total != null
                ? total
                : BigDecimal.ZERO;
    }
}