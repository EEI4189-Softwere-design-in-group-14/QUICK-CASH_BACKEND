package com.quickcash.repository;

import com.quickcash.entity.Disbursement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DisbursementRepository
        extends JpaRepository<Disbursement, Long> {

    Optional<Disbursement> findByLoanLoanId(Long loanId);
}