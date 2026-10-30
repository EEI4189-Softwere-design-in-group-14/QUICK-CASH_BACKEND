package com.quickcash.service;

import com.quickcash.dto.ManagerDashboardResponse;

import com.quickcash.entity.BorrowerStatus;
import com.quickcash.entity.LoanStatus;

import com.quickcash.repository.BorrowerRepository;
import com.quickcash.repository.DisbursementRepository;
import com.quickcash.repository.LoanRepository;
import com.quickcash.repository.PaymentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ManagerDashboardService {

    private final BorrowerRepository borrowerRepository;
    private final LoanRepository loanRepository;
    private final PaymentRepository paymentRepository;
    private final DisbursementRepository disbursementRepository;

    public ManagerDashboardService(
            BorrowerRepository borrowerRepository,
            LoanRepository loanRepository,
            PaymentRepository paymentRepository,
            DisbursementRepository disbursementRepository
    ) {

        this.borrowerRepository =
                borrowerRepository;

        this.loanRepository =
                loanRepository;

        this.paymentRepository =
                paymentRepository;

        this.disbursementRepository =
                disbursementRepository;
    }

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getDashboard() {

        ManagerDashboardResponse response =
                new ManagerDashboardResponse();

        // Borrowers
        response.setTotalBorrowers(
                borrowerRepository.count()
        );

        response.setPendingBorrowers(
                borrowerRepository.countByStatus(
                        BorrowerStatus.PENDING
                )
        );

        response.setApprovedBorrowers(
                borrowerRepository.countByStatus(
                        BorrowerStatus.APPROVED
                )
        );

        // Loans
        response.setTotalLoans(
                loanRepository.count()
        );

        response.setPendingLoans(
                loanRepository.countByStatus(
                        LoanStatus.PENDING
                )
        );

        response.setApprovedLoans(
                loanRepository.countByStatus(
                        LoanStatus.APPROVED
                )
        );

        response.setActiveLoans(
                loanRepository.countByStatus(
                        LoanStatus.ACTIVE
                )
        );

        response.setCompletedLoans(
                loanRepository.countByStatus(
                        LoanStatus.COMPLETED
                )
        );

        // Financial totals
        BigDecimal totalDisbursed =
                disbursementRepository
                        .getTotalDisbursedAmount();

        BigDecimal totalCollected =
                paymentRepository
                        .getTotalCollectedAmount();

        BigDecimal totalOutstanding =
                loanRepository
                        .getTotalOutstandingBalance();

        response.setTotalDisbursed(
                totalDisbursed != null
                        ? totalDisbursed
                        : BigDecimal.ZERO
        );

        response.setTotalCollected(
                totalCollected != null
                        ? totalCollected
                        : BigDecimal.ZERO
        );

        response.setTotalOutstanding(
                totalOutstanding != null
                        ? totalOutstanding
                        : BigDecimal.ZERO
        );

        return response;
    }
}