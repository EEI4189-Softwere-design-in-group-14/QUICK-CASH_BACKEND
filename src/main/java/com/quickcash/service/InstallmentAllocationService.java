
package com.quickcash.service;

import com.quickcash.entity.Loan;
import com.quickcash.entity.LoanInstallment;
import com.quickcash.repository.LoanInstallmentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InstallmentAllocationService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    InstallmentAllocationService.class
            );

    private final LoanInstallmentRepository installmentRepository;

    public InstallmentAllocationService(
            LoanInstallmentRepository installmentRepository
    ) {
        this.installmentRepository = installmentRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void allocatePayment(
            Loan loan,
            BigDecimal paymentAmount
    ) {

        // Step 1: Validate loan
        if (loan == null || loan.getLoanId() == null) {
            throw new IllegalArgumentException(
                    "Saved loan is required"
            );
        }

        // Step 2: Validate payment amount
        if (paymentAmount == null
                || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment amount must be greater than zero"
            );
        }

        // Step 3: Load loan installments
        List<LoanInstallment> installments =
                installmentRepository
                        .findByLoanLoanIdOrderByInstallmentNumberAsc(
                                loan.getLoanId()
                        );

        // Step 4: Handle missing schedule
        if (installments.isEmpty()) {

            // Daily loans must have an installment schedule
            if ("DAILY_FIXED_TOTAL".equals(
                    loan.getRepaymentPlanType())) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Daily loan installment schedule is missing"
                );
            }

            // Preserve existing legacy monthly loan behavior
            log.warn(
                    "Loan {} has no installment schedule. "
                            + "Payment allocation was skipped.",
                    loan.getLoanCode()
            );

            return;
        }

        // Step 5: Start allocating payment
        BigDecimal remaining = paymentAmount;

        for (LoanInstallment installment : installments) {

            if (remaining.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }

            BigDecimal paid = installment.getPaidAmount();

            if (paid == null) {
                paid = BigDecimal.ZERO;
            }

            if (paid.signum() < 0) {
                throw new IllegalStateException(
                        "Installment paid amount cannot be negative"
                );
            }

            // Principal portion
            BigDecimal principalDue =
                    installment.getPrincipalDue();

            // Interest portion
            BigDecimal interestDue =
                    installment.getInterestDue() == null
                            ? BigDecimal.ZERO
                            : installment.getInterestDue();

            // Validate installment amounts
            if (principalDue == null
                    || principalDue.signum() < 0
                    || interestDue.signum() < 0) {

                throw new IllegalStateException(
                        "Invalid installment amount"
                );
            }

            // Step 6: Total installment
            // Daily loan: principal + interest
            // Monthly loan: principal + 0 interest
            BigDecimal totalDue =
                    principalDue.add(interestDue);

            // Step 7: Calculate unpaid installment amount
            BigDecimal unpaid =
                    totalDue.subtract(paid);

            if (unpaid.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException(
                        "Installment has an invalid paid amount"
                );
            }

            // Skip fully paid installments
            if (unpaid.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // Step 8: Allocate payment
            BigDecimal allocated =
                    remaining.min(unpaid);

            installment.setPaidAmount(
                    paid.add(allocated)
            );

            remaining = remaining.subtract(allocated);
        }

        // Step 9: Reject payment that exceeds remaining dues
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment exceeds the unpaid scheduled amount"
            );
        }

        // Step 10: Save updated installments
        installmentRepository.saveAll(installments);
    }
}
