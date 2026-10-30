package com.quickcash.repository;

import com.quickcash.entity.Disbursement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;

public interface DisbursementRepository
        extends JpaRepository<Disbursement, Long> {

    Optional<Disbursement> findByLoanLoanId(Long loanId);

    @Query("""
       SELECT COALESCE(SUM(d.amount), 0)
       FROM Disbursement d
       WHERE d.status =
       com.quickcash.entity.DisbursementStatus.COMPLETED
       """)
    BigDecimal getTotalDisbursedAmount();
}