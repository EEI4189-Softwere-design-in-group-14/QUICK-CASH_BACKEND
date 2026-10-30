package com.quickcash.service;

import com.quickcash.dto.BorrowerResponse;
import com.quickcash.entity.Borrower;
import com.quickcash.entity.BorrowerStatus;
import com.quickcash.entity.User;
import com.quickcash.entity.UserStatus;
import com.quickcash.repository.BorrowerRepository;
import com.quickcash.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BorrowerService {

    private final BorrowerRepository borrowerRepository;
    private final UserRepository userRepository;

    public BorrowerService(
            BorrowerRepository borrowerRepository,
            UserRepository userRepository
    ) {
        this.borrowerRepository = borrowerRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<BorrowerResponse> getPendingBorrowers() {

        return borrowerRepository
                .findByStatus(BorrowerStatus.PENDING)
                .stream()
                .map(BorrowerResponse::new)
                .toList();
    }

    @Transactional
    public BorrowerResponse approveBorrower(Long borrowerId) {

        Borrower borrower = borrowerRepository
                .findById(borrowerId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Borrower not found"
                        )
                );

        if (borrower.getStatus() != BorrowerStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending borrowers can be approved"
            );
        }

        borrower.setStatus(BorrowerStatus.APPROVED);

        User user = borrower.getUser();
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);
        borrowerRepository.save(borrower);

        return new BorrowerResponse(borrower);
    }

    @Transactional
    public BorrowerResponse rejectBorrower(Long borrowerId) {

        Borrower borrower = borrowerRepository
                .findById(borrowerId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Borrower not found"
                        )
                );

        if (borrower.getStatus() != BorrowerStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending borrowers can be rejected"
            );
        }

        borrower.setStatus(BorrowerStatus.REJECTED);

        User user = borrower.getUser();
        user.setStatus(UserStatus.REJECTED);

        userRepository.save(user);
        borrowerRepository.save(borrower);

        return new BorrowerResponse(borrower);
    }
}