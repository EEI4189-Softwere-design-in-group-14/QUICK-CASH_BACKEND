
package com.quickcash.service;

import com.quickcash.dto.BorrowerDashboardResponse;
import com.quickcash.entity.Loan;
import com.quickcash.entity.LoanInstallment;
import com.quickcash.repository.LoanRepository;
import com.quickcash.repository.LoanInstallmentRepository;
import com.quickcash.repository.NotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class BorrowerDashboardService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final NotificationRepository notificationRepository;

    public BorrowerDashboardService(
            LoanRepository loanRepository,
            LoanInstallmentRepository installmentRepository,
            NotificationRepository notificationRepository
    ) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public BorrowerDashboardResponse getDashboard(String email) {

        List<Loan> loans =
                loanRepository
                        .findByBorrower_User_EmailIgnoreCaseOrderByLoanIdDesc(
                                email
                        );

        List<BorrowerDashboardResponse.LoanCard> loanCards =
                new ArrayList<>();

        BigDecimal activePrincipal = BigDecimal.ZERO;
        BigDecimal activePaid = BigDecimal.ZERO;
        BigDecimal activeOutstanding = BigDecimal.ZERO;

        int activeLoans = 0;

        for (Loan loan : loans) {

            String status = String.valueOf(loan.getStatus());

            BigDecimal principal =
                    zeroIfNull(loan.getApprovedAmount());

            BigDecimal outstanding =
                    zeroIfNull(loan.getOutstandingBalance());

            boolean active = "ACTIVE".equalsIgnoreCase(status);

            boolean completed =
                    "COMPLETED".equalsIgnoreCase(status)
                            || "CLOSED".equalsIgnoreCase(status);

            BigDecimal paid = BigDecimal.ZERO;

            if (active || completed) {
                paid = principal.subtract(outstanding)
                        .max(BigDecimal.ZERO);
            }

            if (active) {
                activeLoans++;

                activePrincipal = activePrincipal.add(principal);
                activePaid = activePaid.add(paid);
                activeOutstanding =
                        activeOutstanding.add(outstanding);
            }

            List<LoanInstallment> installments =
                    installmentRepository
                            .findByLoanLoanIdOrderByInstallmentNumberAsc(
                                    loan.getLoanId()
                            );

            LoanInstallment nextInstallment = installments.stream()
                    .filter(i ->
                            remainingAmount(i)
                                    .compareTo(BigDecimal.ZERO) > 0
                    )
                    .findFirst()
                    .orElse(null);

            Integer nextNumber = null;
            java.time.LocalDate nextDueDate = null;
            BigDecimal nextRemaining = null;

            if (nextInstallment != null) {

                nextNumber =
                        nextInstallment.getInstallmentNumber();

                nextDueDate =
                        nextInstallment.getDueDate();

                nextRemaining =
                        remainingAmount(nextInstallment);
            }

            loanCards.add(
                    new BorrowerDashboardResponse.LoanCard(
                            loan.getLoanId(),
                            loan.getLoanCode(),
                            status,
                            principal,
                            paid,
                            outstanding,
                            nextNumber,
                            nextDueDate,
                            nextRemaining
                    )
            );
        }

        long unreadNotifications =
                notificationRepository
                        .countByRecipient_EmailIgnoreCaseAndReadAtIsNull(
                                email
                        );

        return new BorrowerDashboardResponse(
                email,
                loans.size(),
                activeLoans,
                activePrincipal,
                activePaid,
                activeOutstanding,
                unreadNotifications,
                loanCards
        );
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal remainingAmount(LoanInstallment installment) {

        BigDecimal due =
                zeroIfNull(installment.getPrincipalDue());

        BigDecimal paid =
                zeroIfNull(installment.getPaidAmount());

        return due.subtract(paid).max(BigDecimal.ZERO);
    }
}
