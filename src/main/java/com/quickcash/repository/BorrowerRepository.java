package com.quickcash.repository;

import com.quickcash.entity.Borrower;
import com.quickcash.entity.BorrowerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BorrowerRepository
        extends JpaRepository<Borrower, Long> {

    Optional<Borrower> findByUserUserId(Long userId);

    Optional<Borrower> findByNic(String nic);

    Optional<Borrower> findByBorrowerCode(
            String borrowerCode
    );

    boolean existsByNic(String nic);

    List<Borrower> findByStatus(
            BorrowerStatus status
    );
}
