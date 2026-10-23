
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

        if (paymentAmount == null ||
                paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment amount must be greater than zero"
            );
        }

        List<LoanInstallment> installments =
                installmentRepository
                        .findByLoanLoanIdOrderByInstallmentNumberAsc(
                                loan.getLoanId()
                        );

        // Preserve the existing payment flow for older loans
        // that were created before installment schedules existed.
        if (installments.isEmpty()) {
            log.warn(
                    "Loan {} has no installment schedule. "
                            + "Payment allocation was skipped.",
                    loan.getLoanCode()
            );
            return;
        }

        BigDecimal remaining = paymentAmount;

        for (LoanInstallment installment : installments) {

            if (remaining.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }

            BigDecimal paid = installment.getPaidAmount();

            if (paid == null) {
                paid = BigDecimal.ZERO;
            }

            BigDecimal unpaid =
                    installment.getPrincipalDue().subtract(paid);

            if (unpaid.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException(
                        "Installment has an invalid paid amount"
                );
            }

            if (unpaid.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal allocated = remaining.min(unpaid);

            installment.setPaidAmount(
                    paid.add(allocated)
            );

            remaining = remaining.subtract(allocated);
        }

        // Reject allocation inconsistencies and roll back
        // the enclosing payment transaction.
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment exceeds the unpaid scheduled principal"
            );
        }

        installmentRepository.saveAll(installments);
    }
}
