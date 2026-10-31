
package com.quickcash.service;

import com.quickcash.dto.ManagerOverdueInstallmentResponse;
import com.quickcash.dto.ManagerOverdueSummaryResponse;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ManagerOverdueService {

    private static final ZoneId LK_ZONE =
            ZoneId.of("Asia/Colombo");

    private final LoanInstallmentRepository installmentRepository;
    private final UserRepository userRepository;

    public ManagerOverdueService(
            LoanInstallmentRepository installmentRepository,
            UserRepository userRepository
    ) {
        this.installmentRepository = installmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ManagerOverdueInstallmentResponse> getInstallments(
            String managerEmail,
            LocalDate asOf
    ) {
        requireActiveManager(managerEmail);

        LocalDate cutoff = resolveDate(asOf);
        return loadInstallments(cutoff);
    }

    @Transactional(readOnly = true)
    public ManagerOverdueSummaryResponse getSummary(
            String managerEmail,
            LocalDate asOf
    ) {
        requireActiveManager(managerEmail);

        LocalDate cutoff = resolveDate(asOf);
        List<ManagerOverdueInstallmentResponse> rows =
                loadInstallments(cutoff);

        Set<Long> loanIds = new HashSet<>();

        long count1 = 0;
        long count2 = 0;
        long count3 = 0;

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal amount1 = BigDecimal.ZERO;
        BigDecimal amount2 = BigDecimal.ZERO;
        BigDecimal amount3 = BigDecimal.ZERO;

        for (ManagerOverdueInstallmentResponse row : rows) {

            loanIds.add(row.loanId());
            total = total.add(row.remainingAmount());

            switch (row.agingBucket()) {

                case "DAYS_1_TO_7" -> {
                    count1++;
                    amount1 = amount1.add(row.remainingAmount());
                }

                case "DAYS_8_TO_30" -> {
                    count2++;
                    amount2 = amount2.add(row.remainingAmount());
                }

                case "DAYS_31_PLUS" -> {
                    count3++;
                    amount3 = amount3.add(row.remainingAmount());
                }

                default -> throw new IllegalStateException(
                        "Unexpected aging bucket"
                );
            }
        }

        return new ManagerOverdueSummaryResponse(
                cutoff,
                rows.size(),
                loanIds.size(),
                total,
                count1,
                amount1,
                count2,
                amount2,
                count3,
                amount3
        );
    }

    private List<ManagerOverdueInstallmentResponse> loadInstallments(
            LocalDate cutoff
    ) {
        return installmentRepository
                .findManagerOverdueInstallments(
                        LoanStatus.ACTIVE,
                        cutoff
                )
                .stream()
                .map(row ->
                        ManagerOverdueInstallmentResponse.from(
                                row, cutoff
                        )
                )
                .toList();
    }

    private LocalDate resolveDate(LocalDate asOf) {
        return asOf == null
                ? LocalDate.now(LK_ZONE)
                : asOf;
    }

    private void requireActiveManager(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Manager or owner account required"
                        )
                );

        boolean allowedRole =
                user.getRole() == Role.MANAGER
                        || user.getRole() == Role.OWNER;

        if (!allowedRole
                || user.getStatus() != UserStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Active manager or owner required"
            );
        }
    }
}
