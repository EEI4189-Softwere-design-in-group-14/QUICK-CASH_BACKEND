
package com.quickcash.service;

import com.quickcash.dto.CollectorBorrowerSearchResponse;
import com.quickcash.entity.*;
import com.quickcash.repository.LoanRepository;
import com.quickcash.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectorBorrowerSearchServiceTest {

    private static final String COLLECTOR_A =
            "collector-a@test.com";

    private static final String COLLECTOR_B =
            "collector-b@test.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private CollectorBorrowerSearchService searchService;

    // Create a sample collector account
    private User collector(Long id, String email) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        user.setFullName("Test Collector");
        user.setRole(Role.COLLECTOR);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    // Create a sample loan and borrower
    private Loan createLoan(
            Long loanId,
            String borrowerName,
            BorrowerStatus borrowerStatus,
            UserStatus borrowerUserStatus
    ) {

        User borrowerUser = new User();
        borrowerUser.setUserId(loanId + 100);
        borrowerUser.setFullName(borrowerName);
        borrowerUser.setPhone("0718765432");
        borrowerUser.setRole(Role.BORROWER);
        borrowerUser.setStatus(borrowerUserStatus);

        Borrower borrower = new Borrower();
        borrower.setBorrowerId(loanId);
        borrower.setBorrowerCode("BRW-000006");
        borrower.setUser(borrowerUser);
        borrower.setStatus(borrowerStatus);

        Loan loan = new Loan();
        loan.setLoanId(loanId);
        loan.setLoanCode("LN-5FCD244F");
        loan.setBorrower(borrower);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setOutstandingBalance(
                new BigDecimal("45993.00")
        );

        return loan;
    }

    // Mock the logged-in collector and assigned loans
    private void setupCollectorLoans(
            User user,
            List<Loan> loans
    ) {

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        when(loanRepository
                .findByAssignedCollector_UserIdAndStatusOrderByLoanIdDesc(
                        user.getUserId(),
                        LoanStatus.ACTIVE
                ))
                .thenReturn(loans);
    }

    // TEST 1: Search assigned borrower by name
    @Test
    void searchAssignedBorrowerByName() {

        User user = collector(10L, COLLECTOR_A);

        Loan loan = createLoan(
                2L,
                "Nimal Perera",
                BorrowerStatus.APPROVED,
                UserStatus.ACTIVE
        );

        setupCollectorLoans(user, List.of(loan));

        List<CollectorBorrowerSearchResponse> results =
                searchService.searchBorrowers(
                        COLLECTOR_A,
                        "nimal"
                );

        assertEquals(1, results.size());
        assertEquals(
                "Nimal Perera",
                results.get(0).borrowerName()
        );
        assertEquals(2L, results.get(0).loanId());
        assertEquals(
                new BigDecimal("45993.00"),
                results.get(0).outstandingBalance()
        );
    }

    // TEST 2: Search by phone, borrower code and loan code
    @Test
    void searchUsingDifferentFields() {

        User user = collector(10L, COLLECTOR_A);

        Loan loan = createLoan(
                2L,
                "Nimal Perera",
                BorrowerStatus.APPROVED,
                UserStatus.ACTIVE
        );

        setupCollectorLoans(user, List.of(loan));

        assertEquals(
                1,
                searchService.searchBorrowers(
                        COLLECTOR_A,
                        "071876"
                ).size()
        );

        assertEquals(
                1,
                searchService.searchBorrowers(
                        COLLECTOR_A,
                        "brw-000006"
                ).size()
        );

        assertEquals(
                1,
                searchService.searchBorrowers(
                        COLLECTOR_A,
                        "ln-5fcd244f"
                ).size()
        );
    }

    // TEST 3: No matching borrower
    @Test
    void noMatchingBorrowerReturnsEmptyList() {

        User user = collector(10L, COLLECTOR_A);

        Loan loan = createLoan(
                2L,
                "Nimal Perera",
                BorrowerStatus.APPROVED,
                UserStatus.ACTIVE
        );

        setupCollectorLoans(user, List.of(loan));

        List<CollectorBorrowerSearchResponse> results =
                searchService.searchBorrowers(
                        COLLECTOR_A,
                        "zzxyz"
                );

        assertTrue(results.isEmpty());
    }

    // TEST 4: Collector cannot see another collector's borrower

    @Test
    void collectorCannotSearchOtherCollectorsBorrower() {

        User userA = collector(5L, COLLECTOR_A);
        User userB = collector(10L, COLLECTOR_B);

        Loan loanA = createLoan(
                1L,
                "Kamal Silva",
                BorrowerStatus.APPROVED,
                UserStatus.ACTIVE
        );

        Loan loanB = createLoan(
                2L,
                "Nimal Perera",
                BorrowerStatus.APPROVED,
                UserStatus.ACTIVE
        );

        // Collector A can access only Loan A
        setupCollectorLoans(userA, List.of(loanA));

        // Collector B can access only Loan B
        setupCollectorLoans(userB, List.of(loanB));

        // Collector A searches for Collector B's borrower
        List<CollectorBorrowerSearchResponse> resultsA =
                searchService.searchBorrowers(
                        COLLECTOR_A,
                        "Nimal"
                );

        assertTrue(resultsA.isEmpty());

        // Collector B searches for Collector A's borrower
        List<CollectorBorrowerSearchResponse> resultsB =
                searchService.searchBorrowers(
                        COLLECTOR_B,
                        "Kamal"
                );

        assertTrue(resultsB.isEmpty());

        // Verify each collector's assigned loan query
        verify(loanRepository)
                .findByAssignedCollector_UserIdAndStatusOrderByLoanIdDesc(
                        5L,
                        LoanStatus.ACTIVE
                );

        verify(loanRepository)
                .findByAssignedCollector_UserIdAndStatusOrderByLoanIdDesc(
                        10L,
                        LoanStatus.ACTIVE
                );
    }

    // TEST 5: Empty and invalid search queries
    @Test
    void invalidSearchQueryReturnsBadRequest() {

        String[] invalidQueries = {
                "",
                " ",
                "a",
                "x".repeat(81)
        };

        for (String query : invalidQueries) {

            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> searchService.searchBorrowers(
                            COLLECTOR_A,
                            query
                    )
            );

            assertEquals(
                    HttpStatus.BAD_REQUEST,
                    error.getStatusCode()
            );
        }

        ResponseStatusException nullError = assertThrows(
                ResponseStatusException.class,
                () -> searchService.searchBorrowers(
                        COLLECTOR_A,
                        null
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                nullError.getStatusCode()
        );

        verifyNoInteractions(
                userRepository,
                loanRepository
        );
    }

    // TEST 6: Unknown collector is denied
    @Test
    void unknownCollectorIsDenied() {

        when(userRepository.findByEmail(COLLECTOR_A))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> searchService.searchBorrowers(
                        COLLECTOR_A,
                        "Nimal"
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                error.getStatusCode()
        );

        verifyNoInteractions(loanRepository);
    }

    // TEST 7: Manager cannot use collector search service
    @Test
    void managerCannotSearchCollectorBorrowers() {

        User manager = collector(20L, COLLECTOR_A);
        manager.setRole(Role.MANAGER);

        when(userRepository.findByEmail(COLLECTOR_A))
                .thenReturn(Optional.of(manager));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> searchService.searchBorrowers(
                        COLLECTOR_A,
                        "Nimal"
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                error.getStatusCode()
        );

        verifyNoInteractions(loanRepository);
    }

    // TEST 8: Suspended collector cannot search
    @Test
    void suspendedCollectorCannotSearch() {

        User user = collector(10L, COLLECTOR_A);
        user.setStatus(UserStatus.SUSPENDED);

        when(userRepository.findByEmail(COLLECTOR_A))
                .thenReturn(Optional.of(user));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> searchService.searchBorrowers(
                        COLLECTOR_A,
                        "Nimal"
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                error.getStatusCode()
        );

        verifyNoInteractions(loanRepository);
    }

    // TEST 9: Unapproved or inactive borrowers are excluded
    @Test
    void unapprovedAndInactiveBorrowersAreExcluded() {

        User user = collector(10L, COLLECTOR_A);

        Loan approvedLoan = createLoan(
                2L,
                "Nimal Perera",
                BorrowerStatus.APPROVED,
                UserStatus.ACTIVE
        );

        Loan pendingLoan = createLoan(
                3L,
                "Nimal Pending",
                BorrowerStatus.PENDING,
                UserStatus.ACTIVE
        );

        Loan rejectedLoan = createLoan(
                4L,
                "Nimal Rejected",
                BorrowerStatus.REJECTED,
                UserStatus.ACTIVE
        );

        Loan inactiveLoan = createLoan(
                5L,
                "Nimal Inactive",
                BorrowerStatus.APPROVED,
                UserStatus.SUSPENDED
        );

        setupCollectorLoans(
                user,
                List.of(
                        approvedLoan,
                        pendingLoan,
                        rejectedLoan,
                        inactiveLoan
                )
        );

        List<CollectorBorrowerSearchResponse> results =
                searchService.searchBorrowers(
                        COLLECTOR_A,
                        "Nimal"
                );

        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).loanId());
    }

    // TEST 10: Search returns maximum 50 results
    @Test
    void searchReturnsMaximumFiftyResults() {

        User user = collector(10L, COLLECTOR_A);

        List<Loan> loans = new ArrayList<>();

        for (long i = 1; i <= 55; i++) {
            loans.add(
                    createLoan(
                            i,
                            "Nimal Borrower",
                            BorrowerStatus.APPROVED,
                            UserStatus.ACTIVE
                    )
            );
        }

        setupCollectorLoans(user, loans);

        List<CollectorBorrowerSearchResponse> results =
                searchService.searchBorrowers(
                        COLLECTOR_A,
                        "Nimal"
                );

        assertEquals(50, results.size());
    }
}
