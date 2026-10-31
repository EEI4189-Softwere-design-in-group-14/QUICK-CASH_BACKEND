
package com.quickcash.service;

import com.quickcash.dto.CreateLoanFollowUpRequest;
import com.quickcash.dto.LoanFollowUpResponse;
import com.quickcash.entity.*;

import com.quickcash.repository.LoanFollowUpRepository;
import com.quickcash.repository.LoanRepository;
import com.quickcash.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class LoanFollowUpService {

    private static final ZoneId LK_ZONE =
            ZoneId.of("Asia/Colombo");

    private final LoanFollowUpRepository followUpRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public LoanFollowUpService(
            LoanFollowUpRepository followUpRepository,
            LoanRepository loanRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.followUpRepository = followUpRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public LoanFollowUpResponse create(
            Long loanId,
            String collectorEmail,
            CreateLoanFollowUpRequest request
    ) {
        User collector = requireUser(
                collectorEmail, Role.COLLECTOR
        );

        // Lock loan so reassignment cannot race with note creation.
        Loan loan = loanRepository.findByIdForUpdate(loanId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Assigned loan not found"
                ));

        requireAssignedActiveLoan(loan, collector);

        LocalDate nextDate = request.nextFollowUpDate();

        if (nextDate != null
                && nextDate.isBefore(LocalDate.now(LK_ZONE))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Next follow-up date cannot be in the past"
            );
        }

        LoanFollowUp note = new LoanFollowUp();

        note.setLoan(loan);
        note.setCollector(collector);
        note.setOutcome(request.outcome());
        note.setNote(request.note().trim());
        note.setNextFollowUpDate(nextDate);

        LoanFollowUp saved = followUpRepository.save(note);

        auditService.record(
                collector,
                AuditAction.FOLLOW_UP_RECORDED,
                "LOAN_FOLLOW_UP",
                saved.getNoteId(),
                loan.getLoanCode(),
                "Collector follow-up recorded for loan "
                        + loan.getLoanCode()
        );

        return LoanFollowUpResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<LoanFollowUpResponse> getForCollector(
            Long loanId,
            String collectorEmail
    ) {
        User collector = requireUser(
                collectorEmail, Role.COLLECTOR
        );

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Assigned loan not found"
                ));

        requireAssignedActiveLoan(loan, collector);

        return loadHistory(loanId);
    }

    @Transactional(readOnly = true)
    public List<LoanFollowUpResponse> getForManager(
            Long loanId,
            String managerEmail
    ) {
        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Manager account required"
                ));

        boolean allowed = manager.getRole() == Role.MANAGER
                || manager.getRole() == Role.OWNER;

        if (!allowed || manager.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Active manager or owner required"
            );
        }

        if (!loanRepository.existsById(loanId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Loan not found"
            );
        }

        return loadHistory(loanId);
    }

    private List<LoanFollowUpResponse> loadHistory(Long loanId) {
        return followUpRepository
                .findByLoanLoanIdOrderByCreatedAtDescNoteIdDesc(loanId)
                .stream()
                .map(LoanFollowUpResponse::from)
                .toList();
    }

    private User requireUser(String email, Role role) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Account not found"
                ));

        if (user.getRole() != role
                || user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Active account required"
            );
        }

        return user;
    }

    private void requireAssignedActiveLoan(
            Loan loan,
            User collector
    ) {
        if (loan.getStatus() != LoanStatus.ACTIVE
                || loan.getAssignedCollector() == null
                || !loan.getAssignedCollector().getUserId()
                .equals(collector.getUserId())) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Assigned loan not found"
            );
        }
    }
}
