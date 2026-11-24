package com.quickcash.repository;

import com.quickcash.entity.Disbursement;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface DisbursementRepository
        extends JpaRepository<Disbursement, Long> {

    Optional<Disbursement> findByLoanLoanId(Long loanId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT d FROM Disbursement d
        WHERE d.disbursementId = :id
        """)
    Optional<Disbursement> findByIdForUpdate(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT d FROM Disbursement d
        WHERE d.loan.loanId = :loanId
        """)
    Optional<Disbursement> findByLoanIdForUpdate(
            @Param("loanId") Long loanId
    );

    @Query("""
        SELECT d.loan.loanId FROM Disbursement d
        WHERE d.disbursementId = :id
        """)
    Optional<Long> findLoanIdByDisbursementId(
            @Param("id") Long id
    );

    @Query("""
        SELECT COALESCE(SUM(d.amount), 0)
        FROM Disbursement d
        WHERE d.status =
        com.quickcash.entity.DisbursementStatus.COMPLETED
        """)
    BigDecimal getTotalDisbursedAmount();
}