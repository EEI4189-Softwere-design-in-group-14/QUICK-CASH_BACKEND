
package com.quickcash.repository;

import com.quickcash.entity.LoanInstallment;
import com.quickcash.entity.LoanStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LoanInstallmentRepository
        extends JpaRepository<LoanInstallment, Long> {

    boolean existsByLoanLoanId(Long loanId);

    List<LoanInstallment>
    findByLoanLoanIdOrderByInstallmentNumberAsc(
            Long loanId
    );

    // Installments due within the selected date range.
    @Query("""
        SELECT i
        FROM LoanInstallment i
        JOIN FETCH i.loan l
        JOIN FETCH l.borrower b
        JOIN FETCH b.user u
        WHERE i.dueDate BETWEEN :startDate AND :endDate
          AND i.paidAmount < i.principalDue
          AND l.status = :status
        ORDER BY i.dueDate ASC
        """)
    List<LoanInstallment> findUnpaidDueBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") LoanStatus status
    );

    // Installments past the due date and not fully paid.
    @Query("""
        SELECT i
        FROM LoanInstallment i
        JOIN FETCH i.loan l
        JOIN FETCH l.borrower b
        JOIN FETCH b.user u
        WHERE i.dueDate < :today
          AND i.paidAmount < i.principalDue
          AND l.status = :status
        ORDER BY i.dueDate ASC
        """)
    List<LoanInstallment> findOverdueUnpaid(
            @Param("today") LocalDate today,
            @Param("status") LoanStatus status
    );


    @Query("""
    SELECT i
    FROM LoanInstallment i
    JOIN FETCH i.loan l
    JOIN FETCH l.borrower b
    JOIN FETCH b.user u
    JOIN l.assignedCollector c
    WHERE c.userId = :collectorId
      AND l.status = :status
      AND i.dueDate <= :asOfDate
      AND i.paidAmount < i.principalDue
    ORDER BY i.dueDate ASC, i.installmentNumber ASC
    """)
    List<LoanInstallment> findDueForCollector(
            @Param("collectorId") Long collectorId,
            @Param("status") LoanStatus status,
            @Param("asOfDate") LocalDate asOfDate
    );

    @Query("""
    SELECT i
    FROM LoanInstallment i
    JOIN FETCH i.loan l
    JOIN FETCH l.borrower b
    JOIN FETCH b.user u
    LEFT JOIN FETCH l.assignedCollector c
    WHERE l.status = :status
      AND i.dueDate < :asOfDate
      AND i.paidAmount < i.principalDue
    ORDER BY i.dueDate ASC, i.installmentNumber ASC
    """)
    List<LoanInstallment> findManagerOverdueInstallments(
            @Param("status") LoanStatus status,
            @Param("asOfDate") LocalDate asOfDate
    );


}
