package com.quickcash.dto;

import com.quickcash.entity.Guarantor;
import com.quickcash.entity.Loan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LoanResponse {

    private Long loanId;
    private String loanCode;

    private Long borrowerId;
    private String borrowerCode;

    private BigDecimal requestedAmount;
    private BigDecimal approvedAmount;

    private String purpose;
    private Integer repaymentMonths;

    private BigDecimal interestRate;
    private BigDecimal outstandingBalance;

    private String status;

    private LocalDateTime applicationDate;

    private String guarantorFullName;
    private String guarantorNic;
    private String guarantorPhone;

    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private String rejectionReason;

    public LoanResponse(
            Loan loan,
            Guarantor guarantor
    ) {

        this.loanId =
                loan.getLoanId();

        this.loanCode =
                loan.getLoanCode();

        this.borrowerId =
                loan.getBorrower()
                        .getBorrowerId();

        this.borrowerCode =
                loan.getBorrower()
                        .getBorrowerCode();

        this.requestedAmount =
                loan.getRequestedAmount();

        this.approvedAmount =
                loan.getApprovedAmount();

        this.purpose =
                loan.getPurpose();

        this.repaymentMonths =
                loan.getRepaymentMonths();

        this.interestRate =
                loan.getInterestRate();

        this.outstandingBalance =
                loan.getOutstandingBalance();

        this.status =
                loan.getStatus().name();

        this.applicationDate =
                loan.getApplicationDate();

        if (guarantor != null) {

            this.guarantorFullName =
                    guarantor.getFullName();

            this.guarantorNic =
                    guarantor.getNic();

            this.guarantorPhone =
                    guarantor.getPhone();

            this.reviewedAt = loan.getReviewedAt();

            if (loan.getReviewedBy() != null) {
                this.reviewedBy =
                        loan.getReviewedBy().getFullName();
            }

            this.rejectionReason =
                    loan.getRejectionReason();
        }
    }

    public Long getLoanId() {
        return loanId;
    }

    public String getLoanCode() {
        return loanCode;
    }

    public Long getBorrowerId() {
        return borrowerId;
    }

    public String getBorrowerCode() {
        return borrowerCode;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public String getPurpose() {
        return purpose;
    }

    public Integer getRepaymentMonths() {
        return repaymentMonths;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getApplicationDate() {
        return applicationDate;
    }

    public String getGuarantorFullName() {
        return guarantorFullName;
    }

    public String getGuarantorNic() {
        return guarantorNic;
    }

    public String getGuarantorPhone() {
        return guarantorPhone;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}