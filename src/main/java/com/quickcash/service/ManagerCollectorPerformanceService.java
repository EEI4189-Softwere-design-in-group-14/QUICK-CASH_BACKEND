
package com.quickcash.service;

import com.quickcash.dto.CollectorDailyPerformanceResponse;

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
public class ManagerCollectorPerformanceService {

    private static final ZoneId LK_ZONE =
            ZoneId.of("Asia/Colombo");

    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final PaymentRepository paymentRepository;

    public ManagerCollectorPerformanceService(
            UserRepository userRepository,
            LoanRepository loanRepository,
            PaymentRepository paymentRepository
    ) {
        this.userRepository = userRepository;
        this.loanRepository = loanRepository;
        this.paymentRepository = paymentRepository;
    }

    // Performance of all collectors
    @Transactional(readOnly = true)
    public List<CollectorDailyPerformanceResponse> getAll(
            String managerEmail,
            LocalDate date
    ) {
        requireActiveManager(managerEmail);

        LocalDate selectedDate = resolveDate(date);

        return userRepository
                .findByRoleOrderByUserIdAsc(Role.COLLECTOR)
                .stream()
                .map(collector ->
                        buildReport(collector, selectedDate)
                )
                .toList();
    }

    // Performance of one collector
    @Transactional(readOnly = true)
    public CollectorDailyPerformanceResponse getOne(
            String managerEmail,
            Long collectorId,
            LocalDate date
    ) {
        requireActiveManager(managerEmail);

        User collector = userRepository.findById(collectorId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Collector not found"
                        )
                );

        if (collector.getRole() != Role.COLLECTOR) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Collector not found"
            );
        }

        return buildReport(collector, resolveDate(date));
    }

    private CollectorDailyPerformanceResponse buildReport(
            User collector,
            LocalDate date
    ) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        // Only accepted payments are included.
        List<Payment> payments = paymentRepository
                .findByCollector_UserIdAndStatusAndPaymentDateGreaterThanEqualAndPaymentDateLessThanOrderByPaymentDateDesc(
                        collector.getUserId(),
                        PaymentStatus.ACCEPTED,
                        start,
                        end
                );

        long onlineCount = 0;
        long offlineCount = 0;

        BigDecimal onlineTotal = BigDecimal.ZERO;
        BigDecimal offlineTotal = BigDecimal.ZERO;

        for (Payment payment : payments) {

            if (payment.getSource() == PaymentSource.ONLINE) {
                onlineCount++;
                onlineTotal = onlineTotal.add(
                        payment.getAmount()
                );

            } else if (
                    payment.getSource() == PaymentSource.OFFLINE_SYNC
            ) {
                offlineCount++;
                offlineTotal = offlineTotal.add(
                        payment.getAmount()
                );
            }
        }

        BigDecimal totalCollected = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long assignedLoans = loanRepository
                .countByAssignedCollector_UserIdAndStatus(
                        collector.getUserId(),
                        LoanStatus.ACTIVE
                );

        return new CollectorDailyPerformanceResponse(
                date,
                collector.getUserId(),
                collector.getFullName(),
                collector.getStatus(),
                assignedLoans,
                payments.size(),
                onlineCount,
                offlineCount,
                totalCollected,
                onlineTotal,
                offlineTotal
        );
    }

    private LocalDate resolveDate(LocalDate date) {
        return date == null
                ? LocalDate.now(LK_ZONE)
                : date;
    }

    private void requireActiveManager(String email) {

        User manager = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Manager or owner account required"
                        )
                );

        boolean allowedRole =
                manager.getRole() == Role.MANAGER
                        || manager.getRole() == Role.OWNER;

        if (!allowedRole
                || manager.getStatus() != UserStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Active manager or owner account required"
            );
        }
    }
}
