package com.quickcash.repository;

import com.quickcash.entity.Payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.repository.query.Param;


public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByClientTransactionId(
            String clientTransactionId
    );

    boolean existsByClientTransactionId(
            String clientTransactionId
    );

    List<Payment> findByLoanLoanIdOrderByPaymentDateDesc(
            Long loanId
    );

    List<Payment> findByLoanBorrowerUserEmailOrderByPaymentDateDesc(
            String email
    );

    @Query("""
       SELECT COALESCE(SUM(p.amount), 0)
       FROM Payment p
       WHERE p.status = com.quickcash.entity.PaymentStatus.ACCEPTED
       """)
    BigDecimal getTotalCollectedAmount();

    List<Payment> findByPaymentDateBetweenOrderByPaymentDateDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("""
       SELECT COALESCE(SUM(p.amount), 0)
       FROM Payment p
       WHERE p.status =
       com.quickcash.entity.PaymentStatus.ACCEPTED
       AND p.paymentDate >= :start
       AND p.paymentDate < :end
       """)
    BigDecimal getCollectedAmountBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}