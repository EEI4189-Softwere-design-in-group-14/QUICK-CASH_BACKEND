package com.quickcash.repository;

import com.quickcash.entity.Payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

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
}