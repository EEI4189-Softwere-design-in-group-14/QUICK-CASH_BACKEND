
package com.quickcash.service;

import com.quickcash.dto.PaymentRequest;
import com.quickcash.dto.PaymentResponse;

import com.quickcash.entity.*;

import com.quickcash.repository.LoanRepository;
import com.quickcash.repository.PaymentRepository;
import com.quickcash.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final ReceiptService receiptService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final InstallmentAllocationService installmentAllocationService;

    public PaymentService(
            PaymentRepository paymentRepository,
            LoanRepository loanRepository,
            UserRepository userRepository,
            ReceiptService receiptService,
            AuditService auditService,
            NotificationService notificationService,
            InstallmentAllocationService installmentAllocationService
    ) {
        this.paymentRepository = paymentRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.receiptService = receiptService;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.installmentAllocationService = installmentAllocationService;
    }

    // ==========================================
    // 1. RECORD ONLINE OR OFFLINE PAYMENT
    // ==========================================

    @Transactional
    public PaymentResponse recordPayment(
            String collectorEmail,
            PaymentRequest request,
            PaymentSource source
    ) {

        // Step 1: Verify the collector account.
        User collector = requireActiveCollector(collectorEmail);

        // Step 2: Verify loan assignment.
        Loan loan = requireAssignedLoanForPayment(
                request.getLoanId(),
                collector
        );

        // Step 3: Validate client transaction ID.
        if (request.getClientTransactionId() == null
                || request.getClientTransactionId().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Client transaction ID is required"
            );
        }

        String transactionId =
                request.getClientTransactionId().trim();

        // Step 4: Validate payment amount.
        BigDecimal paymentAmount = request.getAmount();

        if (paymentAmount == null
                || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment amount must be greater than zero"
            );
        }

        // Step 5: Secure duplicate transaction check.
        var existing = paymentRepository
                .findByClientTransactionId(transactionId);

        if (existing.isPresent()) {

            Payment previousPayment = existing.get();

            boolean sameLoan =
                    previousPayment.getLoan()
                            .getLoanId()
                            .equals(loan.getLoanId());

            boolean sameCollector =
                    previousPayment.getCollector()
                            .getUserId()
                            .equals(collector.getUserId());

            boolean sameAmount =
                    previousPayment.getAmount()
                            .compareTo(paymentAmount) == 0;

            boolean sameSource =
                    previousPayment.getSource() == source;

            // A transaction ID cannot be reused
            // with different payment details.
            if (!sameLoan
                    || !sameCollector
                    || !sameAmount
                    || !sameSource) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Transaction ID already used with different payment details"
                );
            }

            // Valid retry: return original payment.
            // Do not deduct money again.
            receiptService.createReceiptIfMissing(
                    previousPayment
            );

            return new PaymentResponse(previousPayment);
        }

        // Step 6: Check loan status.
        if (loan.getStatus() != LoanStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payments can only be recorded for active loans"
            );
        }

        // Step 7: Check outstanding balance.
        BigDecimal balanceBefore =
                loan.getOutstandingBalance();

        if (balanceBefore == null) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Loan outstanding balance is not initialized"
            );
        }

        if (paymentAmount.compareTo(balanceBefore) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment amount cannot exceed outstanding balance"
            );
        }

        BigDecimal balanceAfter =
                balanceBefore.subtract(paymentAmount);

        // Step 8: Create payment record.
        Payment payment = new Payment();

        payment.setPaymentCode(
                "PAY-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase()
        );

        payment.setClientTransactionId(transactionId);
        payment.setLoan(loan);
        payment.setCollector(collector);

        payment.setAmount(paymentAmount);
        payment.setBalanceBefore(balanceBefore);
        payment.setBalanceAfter(balanceAfter);

        payment.setSource(source);
        payment.setStatus(PaymentStatus.ACCEPTED);

        payment.setPaymentDate(
                request.getPaymentDate() != null
                        ? request.getPaymentDate()
                        : LocalDateTime.now()
        );

        // Step 9: Update loan balance.
        loan.setOutstandingBalance(balanceAfter);

        if (balanceAfter.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.COMPLETED);
        }

        loanRepository.save(loan);

        // Step 10: Save payment.
        Payment savedPayment =
                paymentRepository.save(payment);

        // Step 11: Allocate payment to installments.
        installmentAllocationService.allocatePayment(
                loan,
                savedPayment.getAmount()
        );

        // Step 12: Record audit log.
        auditService.record(
                collector,
                AuditAction.PAYMENT_RECORDED,
                "PAYMENT",
                savedPayment.getPaymentId(),
                savedPayment.getPaymentCode(),
                "Payment of LKR "
                        + savedPayment.getAmount()
                        + " recorded for loan "
                        + loan.getLoanCode()
        );

        // Step 13: Create receipt.
        receiptService.createReceiptIfMissing(
                savedPayment
        );

        // Step 14: Notify borrower.
        notificationService.send(
                loan.getBorrower().getUser(),
                NotificationType.PAYMENT_RECEIVED,
                "Payment Received",
                "Your payment of LKR "
                        + savedPayment.getAmount()
                        + " for loan "
                        + loan.getLoanCode()
                        + " was recorded successfully. "
                        + "Remaining balance: LKR "
                        + loan.getOutstandingBalance()
        );

        return new PaymentResponse(savedPayment);
    }

    // ==========================================
    // 2. COLLECTOR PAYMENT HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public List<PaymentResponse> getLoanPayments(
            Long loanId,
            String collectorEmail
    ) {

        User collector =
                requireActiveCollector(collectorEmail);

        requireAssignedLoan(loanId, collector);

        return paymentRepository
                .findByLoanLoanIdOrderByPaymentDateDesc(loanId)
                .stream()
                .map(PaymentResponse::new)
                .toList();
    }

    // ==========================================
    // 3. BORROWER PAYMENT HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(
            String borrowerEmail
    ) {

        return paymentRepository
                .findByLoanBorrowerUserEmailOrderByPaymentDateDesc(
                        borrowerEmail
                )
                .stream()
                .map(PaymentResponse::new)
                .toList();
    }

    // ==========================================
    // 4. VALIDATE ACTIVE COLLECTOR
    // ==========================================

    private User requireActiveCollector(
            String collectorEmail
    ) {

        User collector = userRepository
                .findByEmail(collectorEmail)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Active collector account required"
                        )
                );

        if (collector.getRole() != Role.COLLECTOR
                || collector.getStatus() != UserStatus.ACTIVE) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Active collector account required"
            );
        }

        return collector;
    }

    // ==========================================
    // 5. VALIDATE LOAN ASSIGNMENT
    // ==========================================

    private Loan requireAssignedLoan(
            Long loanId,
            User collector
    ) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Assigned loan not found"
                        )
                );

        if (loan.getAssignedCollector() == null
                || !loan.getAssignedCollector()
                .getUserId()
                .equals(collector.getUserId())) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Assigned loan not found"
            );
        }

        return loan;
    }


    private Loan requireAssignedLoanForPayment(
            Long loanId,
            User collector
    ) {

        // Lock this loan during payment processing.
        Loan loan = loanRepository.findByIdForUpdate(loanId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Assigned loan not found"
                        )
                );

        // Verify the current collector assignment.
        if (loan.getAssignedCollector() == null
                || !loan.getAssignedCollector()
                .getUserId()
                .equals(collector.getUserId())) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Assigned loan not found"
            );
        }

        return loan;
    }

}
