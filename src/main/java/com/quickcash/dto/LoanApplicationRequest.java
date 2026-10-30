package com.quickcash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class LoanApplicationRequest {

    @NotNull(message = "Requested amount is required")
    @DecimalMin(
            value = "1.00",
            message = "Requested amount must be greater than zero"
    )
    private BigDecimal requestedAmount;

    @NotBlank(message = "Loan purpose is required")
    private String purpose;

    @NotNull(message = "Repayment period is required")
    @Min(
            value = 1,
            message = "Repayment period must be at least 1 month"
    )
    private Integer repaymentMonths;


    // Guarantor Details

    @NotBlank(message = "Guarantor full name is required")
    private String guarantorFullName;

    @NotBlank(message = "Guarantor NIC is required")
    @Pattern(
            regexp = "^(\\d{12}|\\d{9}[VvXx])$",
            message = "Enter a valid Sri Lankan NIC"
    )
    private String guarantorNic;

    @NotBlank(message = "Guarantor phone is required")
    @Pattern(
            regexp = "^(?:\\+94|0)\\d{9}$",
            message = "Enter a valid Sri Lankan phone number"
    )
    private String guarantorPhone;

    @NotBlank(message = "Guarantor address is required")
    private String guarantorAddress;

    private String guarantorOccupation;


    public LoanApplicationRequest() {
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(
            BigDecimal requestedAmount
    ) {
        this.requestedAmount = requestedAmount;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Integer getRepaymentMonths() {
        return repaymentMonths;
    }

    public void setRepaymentMonths(
            Integer repaymentMonths
    ) {
        this.repaymentMonths = repaymentMonths;
    }

    public String getGuarantorFullName() {
        return guarantorFullName;
    }

    public void setGuarantorFullName(
            String guarantorFullName
    ) {
        this.guarantorFullName =
                guarantorFullName;
    }

    public String getGuarantorNic() {
        return guarantorNic;
    }

    public void setGuarantorNic(
            String guarantorNic
    ) {
        this.guarantorNic = guarantorNic;
    }

    public String getGuarantorPhone() {
        return guarantorPhone;
    }

    public void setGuarantorPhone(
            String guarantorPhone
    ) {
        this.guarantorPhone =
                guarantorPhone;
    }

    public String getGuarantorAddress() {
        return guarantorAddress;
    }

    public void setGuarantorAddress(
            String guarantorAddress
    ) {
        this.guarantorAddress =
                guarantorAddress;
    }

    public String getGuarantorOccupation() {
        return guarantorOccupation;
    }

    public void setGuarantorOccupation(
            String guarantorOccupation
    ) {
        this.guarantorOccupation =
                guarantorOccupation;
    }
}