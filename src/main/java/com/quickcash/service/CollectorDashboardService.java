
package com.quickcash.service;

import com.quickcash.dto.CollectorDashboardResponse;
import com.quickcash.dto.PaymentResponse;

import com.quickcash.entity.*;

import com.quickcash.repository.LoanRepository;
import com.quickcash.repository.PaymentRepository;
import com.quickcash.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class CollectorDashboardService {

    private static final ZoneId SRI_LANKA_ZONE =
            ZoneId.of("Asia/Colombo");

    private final LoanRepository loanRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public CollectorDashboardService(
            LoanRepository loanRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository
    ) {
        this.loanRepository = loanRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    // Collector dashboard
    @Transactional(readOnly = true)
    public CollectorDashboardResponse getDashboard(
            String collectorEmail
    ) {

        User collector = requireActiveCollector(collectorEmail);

        LocalDate today = LocalDate.now(SRI_LANKA_ZONE);

        List<Loan> assignedLoans = loanRepository
                .findByAssignedCollector_UserIdAndStatusOrderByLoanIdDesc(
                        collector.getUserId(),
                        LoanStatus.ACTIVE
                );

        BigDecimal totalOutstanding = assignedLoans
                .stream()
                .map(Loan::getOutstandingBalance)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Payment> todayPayments =
                findTodayAcceptedPayments(collector, today);

        BigDecimal collectedToday = todayPayments
                .stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CollectorDashboardResponse(
                today,
                assignedLoans.size(),
                totalOutstanding,
                todayPayments.size(),
                collectedToday
        );
    }

    // Today's accepted payment records
    @Transactional(readOnly = true)
    public List<PaymentResponse> getTodayCollections(
            String collectorEmail
    ) {

        User collector = requireActiveCollector(collectorEmail);

        LocalDate today = LocalDate.now(SRI_LANKA_ZONE);

        return findTodayAcceptedPayments(collector, today)
                .stream()
                .map(PaymentResponse::new)
                .toList();
    }

    private List<Payment> findTodayAcceptedPayments(
            User collector,
            LocalDate date
    ) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        return paymentRepository
                .findByCollector_UserIdAndStatusAndPaymentDateGreaterThanEqualAndPaymentDateLessThanOrderByPaymentDateDesc(
                        collector.getUserId(),
                        PaymentStatus.ACCEPTED,
                        start,
                        end
                );
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
