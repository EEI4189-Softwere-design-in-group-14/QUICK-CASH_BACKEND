
package com.quickcash.service;

import com.quickcash.dto.CollectorDueInstallmentResponse;
import com.quickcash.dto.CollectorDueSummaryResponse;
import com.quickcash.entity.*;
import com.quickcash.repository.LoanInstallmentRepository;
import com.quickcash.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class CollectorDueService {

    private static final ZoneId LK_ZONE =
            ZoneId.of("Asia/Colombo");

    private final LoanInstallmentRepository installmentRepository;
    private final UserRepository userRepository;

    public CollectorDueService(
            LoanInstallmentRepository installmentRepository,
            UserRepository userRepository
    ) {
        this.installmentRepository = installmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CollectorDueInstallmentResponse> getDueInstallments(
            String collectorEmail,
            LocalDate asOfDate
    ) {
        User collector = requireActiveCollector(collectorEmail);

        LocalDate cutoff = resolveDate(asOfDate);

        return loadDueInstallments(collector, cutoff);
    }

    @Transactional(readOnly = true)
    public CollectorDueSummaryResponse getDueSummary(
            String collectorEmail,
            LocalDate asOfDate
    ) {
        User collector = requireActiveCollector(collectorEmail);
        LocalDate cutoff = resolveDate(asOfDate);

        List<CollectorDueInstallmentResponse> installments =
                loadDueInstallments(collector, cutoff);

        long dueOnDateCount = installments.stream()
                .filter(i -> i.dueDate().isEqual(cutoff))
                .count();

        long overdueCount = installments.stream()
                .filter(i -> i.dueDate().isBefore(cutoff))
                .count();

        BigDecimal totalRemainingDue = installments.stream()
                .map(CollectorDueInstallmentResponse::remainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CollectorDueSummaryResponse(
                cutoff,
                dueOnDateCount,
                overdueCount,
                totalRemainingDue
        );
    }

    private List<CollectorDueInstallmentResponse> loadDueInstallments(
            User collector,
            LocalDate cutoff
    ) {
        return installmentRepository
                .findDueForCollector(
                        collector.getUserId(),
                        LoanStatus.ACTIVE,
                        cutoff
                )
                .stream()
                .map(i -> CollectorDueInstallmentResponse.from(
                        i, cutoff
                ))
                .toList();
    }

    private LocalDate resolveDate(LocalDate asOfDate) {
        return asOfDate == null
                ? LocalDate.now(LK_ZONE)
                : asOfDate;
    }

    private User requireActiveCollector(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Collector account required"
                        )
                );

        if (user.getRole() != Role.COLLECTOR
                || user.getStatus() != UserStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Active collector account required"
            );
        }

        return user;
    }
}
