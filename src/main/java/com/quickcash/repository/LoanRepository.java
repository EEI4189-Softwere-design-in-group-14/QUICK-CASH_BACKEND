package com.quickcash.repository;

import com.quickcash.entity.Loan;
import com.quickcash.entity.LoanStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoanRepository
        extends JpaRepository<Loan, Long> {

    Optional<Loan> findByLoanCode(String loanCode);

    List<Loan> findByBorrowerBorrowerId(
            Long borrowerId
    );

    List<Loan> findByStatus(
            LoanStatus status
    );

    boolean existsByBorrowerBorrowerIdAndStatusIn(
            Long borrowerId,
            Collection<LoanStatus> statuses
    );
}