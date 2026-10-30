
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
import com.quickcash.entity.NotificationType;

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

    @Transactional
    public PaymentResponse recordPayment(
            String collectorEmail,
            PaymentRequest request,
            PaymentSource source
    ) {

        // Prevent duplicate online or offline transactions.
        var existing = paymentRepository.findByClientTransactionId(
                request.getClientTransactionId().trim()
        );

        if (existing.isPresent()) {
            receiptService.createReceiptIfMissing(existing.get());

            return new PaymentResponse(existing.get());
        }

        // Find the collector.
        User collector = userRepository.findByEmail(collectorEmail)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Collector not found"
                        )
                );

        if (collector.getRole() != Role.COLLECTOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only field collectors can record payments"
            );
        }

        // Find the loan.
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Loan not found"
                        )
                );

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payments can only be recorded for active loans"
            );
        }

        BigDecimal balanceBefore = loan.getOutstandingBalance();

        if (balanceBefore == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Loan outstanding balance is not initialized"
            );
        }

        BigDecimal paymentAmount = request.getAmount();

        if (paymentAmount.compareTo(balanceBefore) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment amount cannot exceed outstanding balance"
            );
        }

        BigDecimal balanceAfter = balanceBefore.subtract(paymentAmount);

        // Create the payment record.
        Payment payment = new Payment();

        payment.setPaymentCode(
                "PAY-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase()
        );

        payment.setClientTransactionId(
                request.getClientTransactionId().trim()
        );

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

        // Update the outstanding loan balance.
        loan.setOutstandingBalance(balanceAfter);

        if (balanceAfter.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.COMPLETED);
        }

        loanRepository.save(loan);

        // Save the payment first to obtain its generated ID.
        Payment savedPayment = paymentRepository.save(payment);

        installmentAllocationService.allocatePayment(
                loan,
                savedPayment.getAmount()
        );

        // Record the payment in the audit trail.
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

        // Generate the receipt.
        receiptService.createReceiptIfMissing(savedPayment);

// Notify the borrower about the successful payment.
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

    @Transactional(readOnly = true)
    public List<PaymentResponse> getLoanPayments(Long loanId) {

        return paymentRepository
                .findByLoanLoanIdOrderByPaymentDateDesc(loanId)
                .stream()
                .map(PaymentResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(String borrowerEmail) {

        return paymentRepository
                .findByLoanBorrowerUserEmailOrderByPaymentDateDesc(
                        borrowerEmail
                )
                .stream()
                .map(PaymentResponse::new)
                .toList();
    }
}
