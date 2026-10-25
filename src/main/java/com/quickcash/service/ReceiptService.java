package com.quickcash.service;

import com.quickcash.dto.ReceiptResponse;
import com.quickcash.entity.Payment;
import com.quickcash.entity.Receipt;
import com.quickcash.entity.Role;
import com.quickcash.entity.User;
import com.quickcash.repository.PaymentRepository;
import com.quickcash.repository.ReceiptRepository;
import com.quickcash.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.List;

@Service
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public ReceiptService(
            ReceiptRepository receiptRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository
    ) {
        this.receiptRepository =
                receiptRepository;

        this.paymentRepository =
                paymentRepository;

        this.userRepository =
                userRepository;
    }

    @Transactional
    public ReceiptResponse createReceiptIfMissing(
            Payment payment
    ) {

        var existing =
                receiptRepository
                        .findByPaymentPaymentId(
                                payment.getPaymentId()
                        );

        if (existing.isPresent()) {
            return new ReceiptResponse(
                    existing.get()
            );
        }

        Receipt receipt =
                new Receipt();

        receipt.setPayment(payment);

        receipt.setReceiptNumber(
                generateReceiptNumber()
        );

        Receipt saved =
                receiptRepository.save(receipt);

        return new ReceiptResponse(saved);
    }


    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptByPayment(
            Long paymentId,
            String currentUserEmail
    ) {

        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Payment not found"
                        )
                );

        // Check authorization BEFORE fetching the receipt
        checkAccess(currentUserEmail, payment);

        Receipt receipt = receiptRepository
                .findByPaymentPaymentId(paymentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Receipt not found for this payment"
                        )
                );

        return new ReceiptResponse(receipt);
    }


    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptById(
            Long receiptId,
            String currentUserEmail
    ) {

        Receipt receipt =
                receiptRepository
                        .findById(receiptId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Receipt not found"
                                )
                        );

        checkAccess(
                currentUserEmail,
                receipt.getPayment()
        );

        return new ReceiptResponse(receipt);
    }

    private void checkAccess(
            String email,
            Payment payment
    ) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "User not found"
                                )
                        );

        /*
         * Manager can view all receipts.
         */
        if (user.getRole() == Role.MANAGER) {
            return;
        }

        /*
         * Collector can view receipts
         * for payments collected by them.
         */
        if (user.getRole() == Role.COLLECTOR
                && payment.getCollector()
                .getUserId()
                .equals(user.getUserId())) {

            return;
        }

        /*
         * Borrower can only view their
         * own loan receipts.
         */
        if (user.getRole() == Role.BORROWER
                && payment.getLoan()
                .getBorrower()
                .getUser()
                .getUserId()
                .equals(user.getUserId())) {

            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this receipt"
        );
    }

    private String generateReceiptNumber() {

        String date =
                LocalDate.now()
                        .format(
                                DateTimeFormatter
                                        .ofPattern("yyyyMMdd")
                        );

        String random =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

        return "RCT-" +
                date +
                "-" +
                random;
    }
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getMyReceipts(
            String borrowerEmail
    ) {

        return receiptRepository
                .findByPaymentLoanBorrowerUserEmailOrderByIssuedAtDesc(
                        borrowerEmail
                )
                .stream()
                .map(ReceiptResponse::new)
                .toList();
    }
}