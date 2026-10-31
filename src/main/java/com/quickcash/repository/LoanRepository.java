package com.quickcash.repository;

import com.quickcash.entity.Loan;
import com.quickcash.entity.LoanStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

public interface LoanRepository
        extends JpaRepository<Loan, Long> {

    Optional<Loan> findByLoanCode(String loanCode);

    List<Loan> findByBorrowerBorrowerId(
            Long borrowerId
    );

    List<Loan> findByStatus(
            LoanStatus status
    );


    List<Loan> findByBorrower_User_EmailIgnoreCaseOrderByLoanIdDesc(
            String email
    );


    List<Loan> findByAssignedCollector_UserIdAndStatusOrderByLoanIdDesc(
            Long collectorUserId,
            LoanStatus status
    );



    boolean existsByBorrowerBorrowerIdAndStatusIn(
            Long borrowerId,
            Collection<LoanStatus> statuses
    );
    long countByStatus(LoanStatus status);
    @Query("""
       SELECT COALESCE(SUM(l.outstandingBalance), 0)
       FROM Loan l
       WHERE l.status = com.quickcash.entity.LoanStatus.ACTIVE
       """)
    BigDecimal getTotalOutstandingBalance();


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Loan l WHERE l.loanId = :loanId")
    Optional<Loan> findByIdForUpdate(
            @Param("loanId") Long loanId
    );


    long countByAssignedCollector_UserIdAndStatus(
            Long collectorUserId,
            LoanStatus status
    );

}