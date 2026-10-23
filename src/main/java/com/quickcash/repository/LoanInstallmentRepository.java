
package com.quickcash.repository;

import com.quickcash.entity.LoanInstallment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanInstallmentRepository
        extends JpaRepository<LoanInstallment, Long> {

    boolean existsByLoanLoanId(Long loanId);

    List<LoanInstallment>
    findByLoanLoanIdOrderByInstallmentNumberAsc(
            Long loanId
    );
}
