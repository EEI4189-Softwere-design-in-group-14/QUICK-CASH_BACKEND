package com.quickcash.repository;

import com.quickcash.entity.LoanFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanFollowUpRepository
        extends JpaRepository<LoanFollowUp, Long> {

    List<LoanFollowUp>
    findByLoanLoanIdOrderByCreatedAtDescNoteIdDesc(
            Long loanId
    );
}