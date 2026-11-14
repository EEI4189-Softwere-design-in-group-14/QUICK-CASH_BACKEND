
package com.quickcash.service;

import com.quickcash.dto.CollectorBorrowerSearchResponse;
import com.quickcash.entity.*;
import com.quickcash.repository.LoanRepository;
import com.quickcash.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class CollectorBorrowerSearchService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;

    public CollectorBorrowerSearchService(
            LoanRepository loanRepository,
            UserRepository userRepository
    ) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CollectorBorrowerSearchResponse> searchBorrowers(
            String collectorEmail,
            String query
    ) {

        // 1. Validate the search text.
        if (query == null ||
                query.trim().length() < 2 ||
                query.trim().length() > 80) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Search must contain 2 to 80 characters"
            );
        }

        String searchText =
                query.trim().toLowerCase(Locale.ROOT);

        // 2. Find the logged-in collector.
        User collector = userRepository
                .findByEmail(collectorEmail)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Collector account not found"
                        )
                );

        // 3. Check collector authorization.
        if (collector.getRole() != Role.COLLECTOR ||
                collector.getStatus() != UserStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Active collector account required"
            );
        }

        // 4. Load only this collector's active loans.
        List<Loan> assignedLoans = loanRepository
                .findByAssignedCollector_UserIdAndStatusOrderByLoanIdDesc(
                        collector.getUserId(),
                        LoanStatus.ACTIVE
                );

        // 5. Search authorized borrower records.
        return assignedLoans.stream()
                .filter(loan ->
                        loan.getBorrower().getStatus()
                                == BorrowerStatus.APPROVED
                )
                .filter(loan ->
                        loan.getBorrower().getUser().getStatus()
                                == UserStatus.ACTIVE
                )
                .filter(loan -> matchesSearch(loan, searchText))
                .limit(50)
                .map(loan ->
                        new CollectorBorrowerSearchResponse(
                                loan.getBorrower().getBorrowerId(),
                                loan.getBorrower().getBorrowerCode(),
                                loan.getBorrower().getUser().getFullName(),
                                loan.getBorrower().getUser().getPhone(),
                                loan.getLoanId(),
                                loan.getLoanCode(),
                                loan.getOutstandingBalance()
                        )
                )
                .toList();
    }

    private boolean matchesSearch(
            Loan loan,
            String searchText
    ) {

        Borrower borrower = loan.getBorrower();
        User user = borrower.getUser();

        return contains(user.getFullName(), searchText)
                || contains(user.getPhone(), searchText)
                || contains(borrower.getBorrowerCode(), searchText)
                || contains(loan.getLoanCode(), searchText);
    }

    private boolean contains(
            String value,
            String searchText
    ) {
        return value != null &&
                value.toLowerCase(Locale.ROOT)
                        .contains(searchText);
    }
}
