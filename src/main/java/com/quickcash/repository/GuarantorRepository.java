package com.quickcash.repository;

import com.quickcash.entity.Guarantor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuarantorRepository
        extends JpaRepository<Guarantor, Long> {

    Optional<Guarantor> findByLoanLoanId(Long loanId);
}