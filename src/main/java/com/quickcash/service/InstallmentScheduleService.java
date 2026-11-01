
package com.quickcash.service;

import com.quickcash.entity.Loan;
import com.quickcash.entity.LoanInstallment;
import com.quickcash.repository.LoanInstallmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class InstallmentScheduleService {

    private final LoanInstallmentRepository installmentRepository;

    public InstallmentScheduleService(
            LoanInstallmentRepository installmentRepository
    ) {
        this.installmentRepository = installmentRepository;
    }

    @Transactional
    public void createMonthlySchedule(
            Loan loan,
            LocalDate disbursementDate
    ) {

        if (loan == null || loan.getLoanId() == null) {
            throw new IllegalArgumentException(
                    "Saved loan is required"
            );
        }

        if (disbursementDate == null) {
            throw new IllegalArgumentException(
                    "Disbursement date is required"
            );
        }

        // Prevent duplicate schedules.
        if (installmentRepository.existsByLoanLoanId(
                loan.getLoanId()
        )) {
            return;
        }

        Integer repaymentMonths = loan.getRepaymentMonths();

        BigDecimal principal = loan.getApprovedAmount();

        if (repaymentMonths == null || repaymentMonths <= 0) {
            throw new IllegalStateException(
                    "Repayment months must be greater than zero"
            );
        }

        if (principal == null ||
                principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "Approved amount must be greater than zero"
            );
        }

        // Development prototype: principal-only installments.
        BigDecimal monthlyPrincipal = principal.divide(
                BigDecimal.valueOf(repaymentMonths),
                2,
                RoundingMode.DOWN
        );

        List<LoanInstallment> installments =
                new ArrayList<>();

        for (int i = 1; i <= repaymentMonths; i++) {

            LoanInstallment installment =
                    new LoanInstallment();

            installment.setLoan(loan);

            installment.setInstallmentNumber(i);

            installment.setDueDate(
                    disbursementDate.plusMonths(i)
            );

            // Assign rounding remainder to the last installment.
            BigDecimal amountDue =
                    i == repaymentMonths
                            ? principal.subtract(
                            monthlyPrincipal.multiply(
                                    BigDecimal.valueOf(
                                            repaymentMonths - 1
                                    )
                            )
                    )
                            : monthlyPrincipal;

            installment.setPrincipalDue(amountDue);

            installment.setPaidAmount(
                    BigDecimal.ZERO
            );

            installments.add(installment);
        }

        installmentRepository.saveAll(installments);
    }


    @Transactional
    public void createDailySchedule(
            Loan loan,
            LocalDate disbursementDate
    ) {

        if (loan == null || loan.getLoanId() == null) {
            throw new IllegalArgumentException(
                    "Saved loan is required"
            );
        }

        if (!"DAILY_FIXED_TOTAL".equals(
                loan.getRepaymentPlanType())) {
            throw new IllegalArgumentException(
                    "Daily repayment plan is required"
            );
        }

        if (installmentRepository.existsByLoanLoanId(
                loan.getLoanId())) {
            throw new IllegalStateException(
                    "Loan already has an installment schedule"
            );
        }

        BigDecimal principal = loan.getApprovedAmount();
        BigDecimal total = loan.getTotalRepaymentAmount();
        Integer days = loan.getRepaymentDays();

        if (principal == null || total == null ||
                days == null) {
            throw new IllegalArgumentException(
                    "Daily repayment details are required"
            );
        }

        DailyRepaymentScheduleCalculator calculator =
                new DailyRepaymentScheduleCalculator();

        var schedule = calculator.createSchedule(
                principal,
                total,
                days,
                disbursementDate
        );

        List<LoanInstallment> installments =
                new ArrayList<>();

        BigDecimal cumulativeTotal = BigDecimal.ZERO;
        BigDecimal cumulativePrincipal = BigDecimal.ZERO;

        for (var daily : schedule) {

            BigDecimal dailyTotal = daily.amountDue();

            cumulativeTotal =
                    cumulativeTotal.add(dailyTotal);

            // Calculate the principal portion proportionally.
            BigDecimal principalTarget = cumulativeTotal
                    .multiply(principal)
                    .divide(total, 2, RoundingMode.DOWN);

            BigDecimal principalDue =
                    principalTarget.subtract(cumulativePrincipal);

            BigDecimal interestDue =
                    dailyTotal.subtract(principalDue);

            LoanInstallment installment =
                    new LoanInstallment();

            installment.setLoan(loan);
            installment.setInstallmentNumber(
                    daily.installmentNumber()
            );
            installment.setDueDate(daily.dueDate());

            installment.setPrincipalDue(principalDue);
            installment.setInterestDue(interestDue);
            installment.setPaidAmount(BigDecimal.ZERO);

            installments.add(installment);

            cumulativePrincipal = principalTarget;
        }

        installmentRepository.saveAll(installments);
    }

}
