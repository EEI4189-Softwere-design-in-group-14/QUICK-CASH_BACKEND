package com.quickcash.repository;

import com.quickcash.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReceiptRepository
        extends JpaRepository<Receipt, Long> {

    Optional<Receipt> findByPaymentPaymentId(Long paymentId);

    Optional<Receipt> findByReceiptNumber(String receiptNumber);

    boolean existsByPaymentPaymentId(Long paymentId);

    List<Receipt>
    findByPaymentLoanBorrowerUserEmailOrderByIssuedAtDesc(
            String email
    );
}