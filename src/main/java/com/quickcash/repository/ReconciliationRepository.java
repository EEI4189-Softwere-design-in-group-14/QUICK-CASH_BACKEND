package com.quickcash.repository;

import com.quickcash.entity.Reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReconciliationRepository
        extends JpaRepository<Reconciliation, Long> {

    Optional<Reconciliation>
    findByReconciliationDate(
            LocalDate reconciliationDate
    );

    boolean existsByReconciliationDate(
            LocalDate reconciliationDate
    );

    List<Reconciliation>
    findAllByOrderByReconciliationDateDesc();
}