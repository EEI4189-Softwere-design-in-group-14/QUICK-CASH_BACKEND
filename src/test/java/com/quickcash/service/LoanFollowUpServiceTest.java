
package com.quickcash.service;

import com.quickcash.dto.CreateLoanFollowUpRequest;
import com.quickcash.dto.LoanFollowUpResponse;
import com.quickcash.entity.*;

import com.quickcash.repository.LoanFollowUpRepository;
import com.quickcash.repository.LoanRepository;
import com.quickcash.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanFollowUpServiceTest {

    private static final ZoneId LK_ZONE =
            ZoneId.of("Asia/Colombo");

    @Mock
    private LoanFollowUpRepository followUpRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private LoanFollowUpService service;

    // Helper: create a test user
    private User createUser(
            Long id,
            String email,
            Role role,
            UserStatus status
    ) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        user.setFullName("Test User");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    // Helper: create a test loan
    private Loan createLoan(User assignedCollector) {
        Loan loan = new Loan();
        loan.setLoanId(2L);
        loan.setLoanCode("LN-TEST-2");
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setAssignedCollector(assignedCollector);
        loan.setOutstandingBalance(
                new BigDecimal("45000.00")
        );
        return loan;
    }

    // TEST 1: Assigned collector can create a follow-up.
    @Test
    void assignedCollectorCanCreateFollowUp() {
        User collector = createUser(
                5L,
                "collector@test.com",
                Role.COLLECTOR,
                UserStatus.ACTIVE
        );

        Loan loan = createLoan(collector);

        when(userRepository.findByEmail(
                "collector@test.com"
        )).thenReturn(Optional.of(collector));

        when(loanRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(loan));

        when(followUpRepository.save(
                any(LoanFollowUp.class)
        )).thenAnswer(invocation -> {
            LoanFollowUp note = invocation.getArgument(0);
            ReflectionTestUtils.setField(
                    note, "noteId", 101L
            );
            return note;
        });

        LocalDate tomorrow = LocalDate.now(LK_ZONE)
                .plusDays(1);

        CreateLoanFollowUpRequest request =
                new CreateLoanFollowUpRequest(
                        FollowUpOutcome.CONTACTED,
                        "  Called borrower  ",
                        tomorrow
                );

        LoanFollowUpResponse response =
                service.create(
                        2L,
                        "collector@test.com",
                        request
                );

        assertEquals(101L, response.noteId());
        assertEquals(2L, response.loanId());
        assertEquals(
                "Called borrower",
                response.note()
        );
        assertEquals(
                FollowUpOutcome.CONTACTED,
                response.outcome()
        );
        assertEquals(
                tomorrow,
                response.nextFollowUpDate()
        );

        // Follow-ups must not change loan balances.
        assertEquals(
                new BigDecimal("45000.00"),
                loan.getOutstandingBalance()
        );

        verify(auditService).record(
                eq(collector),
                eq(AuditAction.FOLLOW_UP_RECORDED),
                eq("LOAN_FOLLOW_UP"),
                eq(101L),
                eq("LN-TEST-2"),
                anyString()
        );
    }

    // TEST 2: Another collector cannot create a note.
    @Test
    void unassignedCollectorCannotCreateFollowUp() {
        User collector = createUser(
                7L,
                "other@test.com",
                Role.COLLECTOR,
                UserStatus.ACTIVE
        );

        User assignedCollector = createUser(
                5L,
                "collector@test.com",
                Role.COLLECTOR,
                UserStatus.ACTIVE
        );

        when(userRepository.findByEmail(
                "other@test.com"
        )).thenReturn(Optional.of(collector));

        when(loanRepository.findByIdForUpdate(2L))
                .thenReturn(
                        Optional.of(createLoan(assignedCollector))
                );

        CreateLoanFollowUpRequest request =
                new CreateLoanFollowUpRequest(
                        FollowUpOutcome.NO_ANSWER,
                        "No answer",
                        null
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        2L, "other@test.com", request
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        verifyNoInteractions(
                followUpRepository,
                auditService
        );
    }

    // TEST 3: Suspended collector cannot create notes.
    @Test
    void suspendedCollectorIsRejected() {
        User collector = createUser(
                5L,
                "collector@test.com",
                Role.COLLECTOR,
                UserStatus.SUSPENDED
        );

        when(userRepository.findByEmail(
                "collector@test.com"
        )).thenReturn(Optional.of(collector));

        CreateLoanFollowUpRequest request =
                new CreateLoanFollowUpRequest(
                        FollowUpOutcome.CONTACTED,
                        "Contact attempt",
                        null
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        2L, "collector@test.com", request
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                error.getStatusCode()
        );

        verifyNoInteractions(
                loanRepository,
                followUpRepository,
                auditService
        );
    }

    // TEST 4: A past next-follow-up date is rejected.
    @Test
    void pastNextFollowUpDateIsRejected() {
        User collector = createUser(
                5L,
                "collector@test.com",
                Role.COLLECTOR,
                UserStatus.ACTIVE
        );

        when(userRepository.findByEmail(
                "collector@test.com"
        )).thenReturn(Optional.of(collector));

        when(loanRepository.findByIdForUpdate(2L))
                .thenReturn(
                        Optional.of(createLoan(collector))
                );

        CreateLoanFollowUpRequest request =
                new CreateLoanFollowUpRequest(
                        FollowUpOutcome.PROMISED_TO_PAY,
                        "Borrower promised payment",
                        LocalDate.now(LK_ZONE).minusDays(1)
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.create(
                        2L, "collector@test.com", request
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                error.getStatusCode()
        );

        verifyNoInteractions(
                followUpRepository,
                auditService
        );
    }

    // TEST 5: Active manager can read follow-up history.
    @Test
    void activeManagerCanViewFollowUps() {
        User manager = createUser(
                3L,
                "manager@test.com",
                Role.MANAGER,
                UserStatus.ACTIVE
        );

        when(userRepository.findByEmail(
                "manager@test.com"
        )).thenReturn(Optional.of(manager));

        when(loanRepository.existsById(2L))
                .thenReturn(true);

        when(followUpRepository
                .findByLoanLoanIdOrderByCreatedAtDescNoteIdDesc(2L))
                .thenReturn(List.of());

        List<LoanFollowUpResponse> history =
                service.getForManager(
                        2L,
                        "manager@test.com"
                );

        assertTrue(history.isEmpty());

        verify(followUpRepository)
                .findByLoanLoanIdOrderByCreatedAtDescNoteIdDesc(2L);
    }

    // TEST 6: Borrower cannot access manager history.
    @Test
    void borrowerCannotViewManagerHistory() {
        User borrower = createUser(
                9L,
                "borrower@test.com",
                Role.BORROWER,
                UserStatus.ACTIVE
        );

        when(userRepository.findByEmail(
                "borrower@test.com"
        )).thenReturn(Optional.of(borrower));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.getForManager(
                        2L, "borrower@test.com"
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                error.getStatusCode()
        );

        verifyNoInteractions(
                followUpRepository,
                loanRepository
        );
    }

    // TEST 7: Collector cannot read an unassigned loan.
    @Test
    void collectorCannotViewUnassignedLoanHistory() {
        User collector = createUser(
                7L,
                "other@test.com",
                Role.COLLECTOR,
                UserStatus.ACTIVE
        );

        User assignedCollector = createUser(
                5L,
                "collector@test.com",
                Role.COLLECTOR,
                UserStatus.ACTIVE
        );

        when(userRepository.findByEmail(
                "other@test.com"
        )).thenReturn(Optional.of(collector));

        when(loanRepository.findById(2L))
                .thenReturn(
                        Optional.of(createLoan(assignedCollector))
                );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.getForCollector(
                        2L, "other@test.com"
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                error.getStatusCode()
        );

        verifyNoInteractions(followUpRepository);
    }
}
